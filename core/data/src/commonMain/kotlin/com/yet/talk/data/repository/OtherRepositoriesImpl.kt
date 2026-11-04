package com.yet.talk.data.repository

import com.yet.talk.api.TalkEvent
import com.yet.talk.api.source.DefaultRelays
import com.yet.talk.api.source.MediaRemoteSource
import com.yet.talk.api.source.PostRemoteSource
import com.yet.talk.api.source.SocialRemoteSource
import com.yet.talk.data.EventIngestor
import com.yet.talk.data.FeedPager
import com.yet.talk.data.FetchedPage
import com.yet.talk.data.Publisher
import com.yet.talk.data.RemoteSync
import com.yet.talk.data.Session
import com.yet.talk.data.remote
import com.yet.talk.data.toPost
import com.yet.talk.data.userOf
import com.yet.talk.database.dao.ActivityRecord
import com.yet.talk.database.dao.MaintenanceDao
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.dao.PostFeed
import com.yet.talk.database.dao.PostRecord
import com.yet.talk.database.dao.ProfileDao
import com.yet.talk.database.dao.SocialDao
import com.yet.talk.database.dao.TrendingDao
import com.yet.talk.database.dao.TrendingRecord
import com.yet.talk.domain.model.Account
import com.yet.talk.domain.model.ActivityItem
import com.yet.talk.domain.model.DomainError
import com.yet.talk.domain.model.ImageFile
import com.yet.talk.domain.model.MediaUpload
import com.yet.talk.domain.model.Pager
import com.yet.talk.domain.model.Post
import com.yet.talk.domain.model.PostId
import com.yet.talk.domain.model.ProfileEdit
import com.yet.talk.domain.model.Relay
import com.yet.talk.domain.model.User
import com.yet.talk.domain.model.UserId
import com.yet.talk.domain.model.UserStats
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.ActivityRepository
import com.yet.talk.domain.repository.MediaRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.domain.repository.RelayRepository
import com.yet.talk.domain.repository.TrendingRepository
import com.yet.talk.domain.rule.TrendingRanker
import com.yet.talk.nostr.Nip19
import com.yet.talk.nostr.SecretKey
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.Metadata
import com.yet.talk.nostr.nip.RelayListEntry
import com.yet.talk.nostr.nip.relayList
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

@ContributesBinding(AppScope::class)
@Inject
class AccountRepositoryImpl(
    private val session: Session,
    private val maintenance: MaintenanceDao,
    private val sync: RemoteSync,
    private val scope: CoroutineScope,
) : AccountRepository {

    override val account: Flow<Account?> =
        session.viewer.map { pubkey -> pubkey.takeIf { it.isNotEmpty() }?.let { Account(UserId(it), Nip19.npub(it)) } }

    override suspend fun createAccount(): Result<Account> = Result.success(switchTo(SecretKey.generate()))

    override suspend fun importKey(secretKey: String): Result<Account> {
        val input = secretKey.trim()
        val key = (Nip19.decode(input) as? Nip19.Entity.PrivateKey)?.secretKey ?: SecretKey.fromHex(input)
        return key?.let { Result.success(switchTo(it)) } ?: Result.failure(DomainError.InvalidKey)
    }

    override suspend fun exportSecretKey(): Result<String> =
        session.secretKey()?.let { Result.success(Nip19.nsec(it)) } ?: Result.failure(DomainError.NotSignedIn)

    override suspend fun signOut() {
        session.signOut()
        maintenance.clearUserData()
    }

    private suspend fun switchTo(key: SecretKey): Account {
        // A different identity must not see the previous one's cached lists and drafts.
        if (session.secretKey() != key) maintenance.clearUserData()
        session.signIn(key)
        scope.launch { remote { sync.syncLists(key.publicKey) } }
        return Account(UserId(key.publicKey), Nip19.npub(key.publicKey))
    }
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class ProfileRepositoryImpl(
    private val profiles: ProfileDao,
    private val social: SocialDao,
    private val socialRemote: SocialRemoteSource,
    private val sync: RemoteSync,
    private val ingestor: EventIngestor,
    private val publisher: Publisher,
) : ProfileRepository {
    /** NIP-45 counts are not cached in the database: they are only meaningful fresh. */
    private val counts = MutableStateFlow<Map<String, Pair<Long?, Long?>>>(emptyMap())

    override fun observeUser(id: UserId): Flow<User> = profiles.observe(id.value).map { userOf(id.value, it) }

    override fun observeStats(id: UserId): Flow<UserStats> =
        combine(social.observeFollowingCount(id.value), counts) { following, counts ->
            val (followers, posts) = counts[id.value] ?: (null to null)
            UserStats(followers = followers, following = following.toInt(), posts = posts)
        }

    override suspend fun refresh(id: UserId): Result<Unit> = remote {
        ingestor.ingest(socialRemote.profiles(listOf(id.value)))
        sync.syncLists(id.value)
        val followers = socialRemote.countFollowers(id.value)
        val posts = socialRemote.countPosts(id.value)
        counts.update { it + (id.value to (followers to posts)) }
    }

    override suspend fun updateMyProfile(edit: ProfileEdit): Result<Unit> = remote {
        val metadata = with(edit) { Metadata(name = name, displayName = displayName, about = about, picture = picture, website = website, nip05 = nip05) }
        val event = publisher.sign { metadata.toTemplate(it) }
        ingestor.ingest(listOfNotNull(TalkEvent.parse(event)))
        publisher.publish(event)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@ContributesBinding(AppScope::class)
@Inject
class TrendingRepositoryImpl(
    private val posts: PostDao,
    private val trending: TrendingDao,
    private val postsRemote: PostRemoteSource,
    private val sync: RemoteSync,
    private val session: Session,
    private val publisher: Publisher,
) : TrendingRepository {

    override fun trending(): Pager<Post> = FeedPager(
        observe = { limit ->
            session.viewer.flatMapLatest { viewer -> posts.observeFeed(PostFeed.Trending, viewer, limit) }
                .map { records -> records.map(PostRecord::toPost) }
        },
        // Ranked locally: later pages come from the table, not from relays.
        fetch = { until -> if (until != null) FetchedPage(0, null) else FetchedPage(rebuild(), null) },
    )

    /** Rebuilds the ranking from the last 7 days at most every 3 minutes, like KarpathyTalk's server did. */
    private suspend fun rebuild(): Int {
        val now = publisher.now()
        val computedAt = trending.computedAt()
        if (computedAt != null && now - computedAt < TrendingRanker.REBUILD_INTERVAL.inWholeSeconds) return 0
        val since = now - TrendingRanker.WINDOW.inWholeSeconds
        sync.storeWithContext(postsRemote.since(since, limit = WINDOW_FETCH_LIMIT))

        val candidates = trending.candidates(since).map {
            TrendingRanker.Candidate(PostId(it.postId), UserId(it.author), Instant.fromEpochSeconds(it.createdAt))
        }
        val engagements = trending.engagements(since).mapNotNull {
            val type = when (it.type) {
                "reply" -> TrendingRanker.Engagement.Type.Reply
                "like" -> TrendingRanker.Engagement.Type.Like
                "repost" -> TrendingRanker.Engagement.Type.Repost
                else -> return@mapNotNull null
            }
            TrendingRanker.Engagement(PostId(it.postId), UserId(it.actor), type)
        }
        val ranked = TrendingRanker.rank(candidates, engagements, Instant.fromEpochSeconds(now))
        trending.replace(ranked.map { TrendingRecord(it.rank, it.postId.value, it.score) }, computedAt = now)
        return ranked.size
    }

    private companion object {
        const val WINDOW_FETCH_LIMIT = 500
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@ContributesBinding(AppScope::class)
@Inject
class ActivityRepositoryImpl(
    private val posts: PostDao,
    private val profiles: ProfileDao,
    private val social: SocialDao,
    private val postsRemote: PostRemoteSource,
    private val socialRemote: SocialRemoteSource,
    private val sync: RemoteSync,
    private val ingestor: EventIngestor,
    private val session: Session,
) : ActivityRepository {

    override fun activity(): Pager<ActivityItem> = FeedPager(
        observe = { limit ->
            session.viewer.flatMapLatest { viewer ->
                if (viewer.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    social.observeActivity(viewer, limit).mapLatest { records -> records.mapNotNull { it.toItem(viewer) } }
                }
            }
        },
        fetch = { until -> fetch(until) },
    )

    private suspend fun fetch(until: Long?): FetchedPage {
        val me = session.viewer.first()
        if (me.isEmpty()) return FetchedPage(0, null)
        val events = socialRemote.activity(me, until, Pager.PAGE_SIZE)
        if (until == null) ingestor.ingest(socialRemote.followers(me, FOLLOWERS_LIMIT))
        // The viewer's own posts the activity points at, so they can be shown.
        val targets = events.mapNotNull {
            when (it) {
                is TalkEvent.Reaction -> it.postId
                is TalkEvent.Post -> it.thread.parent ?: it.quoteId
                else -> null
            }
        }.distinct()
        sync.storeWithContext(postsRemote.byIds(targets))
        val replies = events.filterIsInstance<TalkEvent.Post>()
        val newItems = sync.storeWithContext(replies) + ingestor.ingest(events - replies.toSet())
        ingestor.ingest(socialRemote.profiles(events.map { it.event.pubkey }.toSet()))
        return FetchedPage(newItems, events.minOfOrNull { it.event.createdAt })
    }

    private suspend fun ActivityRecord.toItem(viewer: String): ActivityItem? {
        val type = when (type) {
            "reply" -> ActivityItem.Type.Reply
            "quote" -> ActivityItem.Type.Quote
            "repost" -> ActivityItem.Type.Repost
            "like" -> ActivityItem.Type.Like
            "follow" -> ActivityItem.Type.Follow
            else -> return null
        }
        return ActivityItem(
            type = type,
            actor = userOf(actor, profiles.observe(actor).first()),
            createdAt = Instant.fromEpochSeconds(createdAt),
            eventPost = eventPostId?.let { posts.observePost(it, viewer).first()?.toPost() },
            subjectPost = subjectPostId?.let { posts.observePost(it, viewer).first()?.toPost() },
        )
    }

    private companion object {
        const val FOLLOWERS_LIMIT = 100
    }
}

@ContributesBinding(AppScope::class)
@Inject
class MediaRepositoryImpl(
    private val media: MediaRemoteSource,
    private val session: Session,
    private val publisher: Publisher,
) : MediaRepository {
    override suspend fun upload(file: ImageFile): Result<MediaUpload> = remote {
        val blob = media.upload(file.bytes, file.mimeType, session.requireSigner(), publisher.now())
        MediaUpload(url = blob.url, mimeType = blob.type ?: file.mimeType, sha256 = blob.sha256, sizeBytes = blob.size)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@ContributesBinding(AppScope::class)
@Inject
class RelayRepositoryImpl(
    private val social: SocialDao,
    private val ingestor: EventIngestor,
    private val session: Session,
    private val publisher: Publisher,
) : RelayRepository {

    override val relays: Flow<List<Relay>> = session.viewer.flatMapLatest { viewer ->
        val defaults = DefaultRelays.urls.map { Relay(it) }
        if (viewer.isEmpty()) {
            flowOf(defaults)
        } else {
            social.observeRelays(viewer).map { stored -> stored.map { Relay(it.url, it.read, it.write) }.ifEmpty { defaults } }
        }
    }

    override suspend fun setRelays(relays: List<Relay>): Result<Unit> = remote {
        val me = publisher.me()
        val entries = relays.map { RelayListEntry(it.url, it.read, it.write) }
        publishReplaceable(social, ingestor, publisher, me, Kinds.RELAY_LIST) { relayList(entries, it) }
    }
}
