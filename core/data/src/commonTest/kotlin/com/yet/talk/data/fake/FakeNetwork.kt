package com.yet.talk.data.fake

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.source.media.BlobDescriptor
import com.yet.talk.api.source.media.MediaRemoteSource
import com.yet.talk.api.source.github.GistDto
import com.yet.talk.api.source.github.GitHubRemoteSource
import com.yet.talk.api.source.github.RepoDto
import com.yet.talk.api.source.github.UserDto
import com.yet.talk.api.source.post.PostRemoteSource
import com.yet.talk.api.source.publish.PublishRemoteSource
import com.yet.talk.api.source.social.SocialRemoteSource
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.relay.PublishResult
import com.yet.talk.nostr.signer.Signer

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

    val gists = mutableMapOf<String, GistDto>()
    val repos = mutableMapOf<String, RepoDto>()
    var gitHubCalls = 0

    val github = object : GitHubRemoteSource {
        override suspend fun gist(id: String) = gists[id].also { gitHubCalls++ }
        override suspend fun repo(owner: String, name: String) = repos["$owner/$name"].also { gitHubCalls++ }
        override suspend fun user(login: String): UserDto? = null.also { gitHubCalls++ }
    }

    val media = object : MediaRemoteSource {
        override suspend fun upload(bytes: ByteArray, mimeType: String, signer: Signer, createdAt: Long) =
            BlobDescriptor("https://blossom/x", "hash", bytes.size.toLong(), mimeType)
    }
}
