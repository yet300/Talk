package com.yet.talk.data.repository

import com.yet.talk.api.TalkEvent
import com.yet.talk.api.TalkEvents
import com.yet.talk.api.source.PostRemoteSource
import com.yet.talk.data.EventIngestor
import com.yet.talk.data.FeedPager
import com.yet.talk.data.FetchedPage
import com.yet.talk.data.Publisher
import com.yet.talk.data.RemoteSync
import com.yet.talk.data.Session
import com.yet.talk.data.remote
import com.yet.talk.data.toPost
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.dao.PostFeed
import com.yet.talk.database.dao.PostRecord
import com.yet.talk.database.dao.ReactionDao
import com.yet.talk.database.dao.ReactionTypes
import com.yet.talk.database.dao.SocialDao
import com.yet.talk.domain.model.DomainError
import com.yet.talk.domain.model.Pager
import com.yet.talk.domain.model.Post
import com.yet.talk.domain.model.PostId
import com.yet.talk.domain.model.UserId
import com.yet.talk.domain.repository.BookmarkRepository
import com.yet.talk.domain.repository.FollowRepository
import com.yet.talk.domain.repository.ReactionRepository
import com.yet.talk.nostr.Event
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.bookmarkList
import com.yet.talk.nostr.nip.contactList
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@ContributesBinding(AppScope::class)
@Inject
class ReactionRepositoryImpl(
    private val posts: PostDao,
    private val reactions: ReactionDao,
    private val postsRemote: PostRemoteSource,
    private val sync: RemoteSync,
    private val publisher: Publisher,
) : ReactionRepository {

    override suspend fun setLiked(postId: PostId, liked: Boolean): Result<Unit> =
        set(postId, ReactionTypes.LIKE, liked) { target, createdAt -> TalkEvents.like(target, createdAt) }

    override suspend fun setReposted(postId: PostId, reposted: Boolean): Result<Unit> =
        set(postId, ReactionTypes.REPOST, reposted) { target, createdAt -> TalkEvents.repost(target, createdAt) }

    private suspend fun set(
        postId: PostId,
        type: String,
        active: Boolean,
        build: (Event, Long) -> com.yet.talk.nostr.EventTemplate,
    ): Result<Unit> = remote {
        val me = publisher.me()
        val existing = reactions.reactionIds(postId.value, me, type)
        if (active) {
            if (existing.isNotEmpty()) return@remote
            val target = targetEvent(postId)
            val event = publisher.sign { build(target, it) }
            try {
                publisher.storeAndPublish(event)
            } catch (error: Throwable) {
                reactions.delete(listOf(event.id), me) // roll back the optimistic like
                throw error
            }
        } else {
            if (existing.isEmpty()) return@remote
            val kind = if (type == ReactionTypes.LIKE) Kinds.REACTION else Kinds.REPOST
            publisher.publish(publisher.sign { TalkEvents.delete(existing.associateWith { kind }, it) })
            reactions.delete(existing, me)
        }
    }

    private suspend fun targetEvent(postId: PostId): Event {
        posts.eventJson(postId.value)?.let { return Event.fromJson(it) }
        sync.storeWithContext(postsRemote.byIds(listOf(postId.value)))
        return posts.eventJson(postId.value)?.let(Event::fromJson) ?: throw DomainError.NotFound
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@ContributesBinding(AppScope::class)
@Inject
class BookmarkRepositoryImpl(
    private val posts: PostDao,
    private val social: SocialDao,
    private val postsRemote: PostRemoteSource,
    private val sync: RemoteSync,
    private val ingestor: EventIngestor,
    private val session: Session,
    private val publisher: Publisher,
) : BookmarkRepository {

    override suspend fun setBookmarked(postId: PostId, bookmarked: Boolean): Result<Unit> = remote {
        val me = publisher.me()
        sync.ensureListsSynced(me)
        val current = social.bookmarkIds(me)
        val updated = if (bookmarked) current + postId.value else current - postId.value
        if (updated.distinct() == current) return@remote
        publishList(me, Kinds.BOOKMARKS) { bookmarkList(updated, it) }
    }

    override fun bookmarks(): Pager<Post> = FeedPager(
        observe = { limit ->
            session.viewer.flatMapLatest { viewer -> posts.observeFeed(PostFeed.Bookmarks, viewer, limit) }
                .map { records -> records.map(PostRecord::toPost) }
        },
        // The whole list arrives at once; there is nothing older to page in from relays.
        fetch = { until ->
            val viewer = session.viewer.first()
            if (until != null || viewer.isEmpty()) {
                FetchedPage(0, null)
            } else {
                sync.syncLists(viewer)
                FetchedPage(sync.storeWithContext(postsRemote.byIds(social.bookmarkIds(viewer))), null)
            }
        },
    )

    private suspend fun publishList(me: String, kind: Int, build: (Long) -> com.yet.talk.nostr.EventTemplate) =
        publishReplaceable(social, ingestor, publisher, me, kind, build)
}

@OptIn(ExperimentalCoroutinesApi::class)
@ContributesBinding(AppScope::class)
@Inject
class FollowRepositoryImpl(
    private val social: SocialDao,
    private val sync: RemoteSync,
    private val ingestor: EventIngestor,
    private val session: Session,
    private val publisher: Publisher,
) : FollowRepository {

    override fun observeIsFollowing(id: UserId): Flow<Boolean> =
        session.viewer.flatMapLatest { viewer ->
            if (viewer.isEmpty()) flowOf(false) else social.observeIsFollowing(viewer, id.value)
        }

    override suspend fun setFollowing(id: UserId, following: Boolean): Result<Unit> = remote {
        val me = publisher.me()
        if (id.value == me) return@remote
        sync.ensureListsSynced(me)
        val current = social.observeFollowees(me).first()
        val updated = if (following) current + id.value else current - id.value
        if (updated.toSet() == current.toSet()) return@remote
        publishReplaceable(social, ingestor, publisher, me, Kinds.CONTACTS) { contactList(updated, it) }
    }
}

/**
 * Publishes a new version of a replaceable list. Its `created_at` must be newer than the stored
 * one, or this device and relays would ignore it; two edits in one second would otherwise collide.
 */
internal suspend fun publishReplaceable(
    social: SocialDao,
    ingestor: EventIngestor,
    publisher: Publisher,
    me: String,
    kind: Int,
    build: (Long) -> com.yet.talk.nostr.EventTemplate,
) {
    val createdAt = maxOf(publisher.now(), (social.listVersion(me, kind) ?: 0) + 1)
    val event = publisher.sign { build(createdAt) }
    ingestor.ingest(listOfNotNull(TalkEvent.parse(event)))
    publisher.publish(event)
}
