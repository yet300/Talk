package com.yet.talk.data

import com.app.common.AppDispatchers
import com.yet.talk.api.TalkEvent
import com.yet.talk.api.source.BlobDescriptor
import com.yet.talk.api.source.MediaRemoteSource
import com.yet.talk.api.source.PostRemoteSource
import com.yet.talk.api.source.PublishRemoteSource
import com.yet.talk.api.source.SocialRemoteSource
import com.yet.talk.data.repository.AccountRepositoryImpl
import com.yet.talk.data.repository.BookmarkRepositoryImpl
import com.yet.talk.data.repository.FollowRepositoryImpl
import com.yet.talk.data.repository.PostRepositoryImpl
import com.yet.talk.data.repository.ReactionRepositoryImpl
import com.yet.talk.data.repository.TrendingRepositoryImpl
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.SqlMaintenanceDao
import com.yet.talk.database.dao.SqlPostDao
import com.yet.talk.database.dao.SqlProfileDao
import com.yet.talk.database.dao.SqlReactionDao
import com.yet.talk.database.dao.SqlSocialDao
import com.yet.talk.database.dao.SqlTrendingDao
import com.yet.talk.nostr.Event
import com.yet.talk.nostr.Signer
import com.yet.talk.nostr.relay.PublishResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.time.Clock
import kotlin.time.Instant

class FakeKeyVault : KeyVault {
    var stored: String? = null
    override suspend fun read() = stored
    override suspend fun write(secretKeyHex: String) { stored = secretKeyHex }
    override suspend fun clear() { stored = null }
}

class FixedClock(var seconds: Long) : Clock {
    override fun now(): Instant = Instant.fromEpochSeconds(seconds)
}

/** Relays in memory: everything published is stored and served back to queries. */
class FakeNetwork {
    val events = mutableListOf<Event>()
    var accept = true
    val published = mutableListOf<Event>()
    val timelineUntils = mutableListOf<Long?>()

    private fun parsed() = events.mapNotNull(TalkEvent::parse)

    val posts = object : PostRemoteSource {
        override suspend fun timeline(authors: List<String>?, until: Long?, limit: Int): List<TalkEvent.Post> {
            timelineUntils += until
            return parsed().filterIsInstance<TalkEvent.Post>()
                .filter { (authors == null || it.event.pubkey in authors) && (until == null || it.event.createdAt <= until) }
                .sortedByDescending { it.event.createdAt }
                .take(limit)
        }
        override suspend fun since(since: Long, limit: Int) = parsed().filterIsInstance<TalkEvent.Post>().filter { it.event.createdAt > since }
        override suspend fun byIds(ids: Collection<String>) = parsed().filterIsInstance<TalkEvent.Post>().filter { it.event.id in ids }
        override suspend fun thread(rootId: String) = parsed().filterIsInstance<TalkEvent.Post>().filter { it.thread.root == rootId }
        override suspend fun related(postIds: Collection<String>): List<TalkEvent> = parsed().filter { event ->
            event.event.tags.any { it.firstOrNull() == "e" && it.getOrNull(1) in postIds }
        }
    }

    val social = object : SocialRemoteSource {
        override suspend fun profiles(pubkeys: Collection<String>) = parsed().filterIsInstance<TalkEvent.Profile>().filter { it.event.pubkey in pubkeys }
        override suspend fun lists(pubkey: String) = parsed().filter {
            it.event.pubkey == pubkey && (it is TalkEvent.Contacts || it is TalkEvent.Bookmarks || it is TalkEvent.RelayList)
        }
        override suspend fun followers(pubkey: String, limit: Int) = parsed().filterIsInstance<TalkEvent.Contacts>().filter { pubkey in it.pubkeys }
        override suspend fun activity(pubkey: String, until: Long?, limit: Int) = emptyList<TalkEvent>()
        override suspend fun countFollowers(pubkey: String): Long? = null
        override suspend fun countPosts(pubkey: String): Long? = null
    }

    val publisher = PublishRemoteSource { event ->
        published += event
        if (accept) {
            events += event
            PublishResult(setOf("wss://fake"), emptyMap(), emptySet())
        } else {
            PublishResult(emptySet(), mapOf("wss://fake" to "blocked"), emptySet())
        }
    }

    val media = object : MediaRemoteSource {
        override suspend fun upload(bytes: ByteArray, mimeType: String, signer: Signer, createdAt: Long) =
            BlobDescriptor("https://blossom/x", "hash", bytes.size.toLong(), mimeType)
    }
}

/** The data layer wired by hand over an in-memory database, as Metro would wire it. */
class TestData(factory: com.yet.talk.database.driver.SqlDriverFactory, scope: CoroutineScope) {
    val network = FakeNetwork()
    val clock = FixedClock(1_800_000_000)
    val vault = FakeKeyVault()

    private val holder = DatabaseHolder(factory)
    private val dispatchers = AppDispatchers(io = Dispatchers.Default)
    val postDao = SqlPostDao(holder, dispatchers)
    val profileDao = SqlProfileDao(holder, dispatchers)
    val reactionDao = SqlReactionDao(holder, dispatchers)
    val socialDao = SqlSocialDao(holder, dispatchers)
    val trendingDao = SqlTrendingDao(holder, dispatchers)
    val maintenanceDao = SqlMaintenanceDao(holder, dispatchers)

    val session = VaultSession(vault)
    val ingestor = EventIngestor(postDao, profileDao, reactionDao, socialDao)
    val sync = RemoteSync(network.posts, network.social, ingestor)
    val publisher = Publisher(session, network.publisher, ingestor, postDao, clock)

    val accounts = AccountRepositoryImpl(session, maintenanceDao, sync, scope)
    val posts = PostRepositoryImpl(postDao, socialDao, network.posts, sync, ingestor, session, publisher)
    val reactions = ReactionRepositoryImpl(postDao, reactionDao, network.posts, sync, publisher)
    val bookmarks = BookmarkRepositoryImpl(postDao, socialDao, network.posts, sync, ingestor, session, publisher)
    val follows = FollowRepositoryImpl(socialDao, sync, ingestor, session, publisher)
    val trending = TrendingRepositoryImpl(postDao, trendingDao, network.posts, sync, session, publisher)
}
