package com.yet.talk.data.repository

import androidx.paging.PagingData
import androidx.paging.map
import com.yet.talk.api.schema.PostRef
import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.api.schema.TalkSchema
import com.yet.talk.api.source.post.PostRemoteSource
import com.yet.talk.data.mapper.toImeta
import com.yet.talk.data.mapper.toPost
import com.yet.talk.data.mapper.toRevision
import com.yet.talk.data.paging.FeedKeys
import com.yet.talk.data.paging.FeedPaging
import com.yet.talk.data.paging.FetchedPage
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.FollowDao
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.model.PostFeed
import com.yet.talk.database.model.PostRecord
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.database.model.PublishStates
import com.yet.talk.nostr.event.Event
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.Revision
import com.yet.talk.domain.model.post.ThreadReply
import com.yet.talk.domain.repository.Feed
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.rule.FeedRules
import com.yet.talk.nostr.nip.Kinds
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
internal class PostRepositoryImpl(
    private val posts: PostDao,
    private val follows: FollowDao,
    private val postsRemote: PostRemoteSource,
    private val sync: RemoteSync,
    private val ingestor: EventIngestor,
    private val session: Session,
    private val publisher: Publisher,
    private val paging: FeedPaging,
) : PostRepository {

    override fun feed(feed: Feed): Flow<PagingData<Post>> =
        session.viewer.flatMapLatest { viewer ->
            paging.feed(
                feedKey = feed.key(viewer),
                pages = { posts.feedPages(feed.toDao(), viewer) },
                fetcher = { until -> fetchFeed(feed, viewer, until) },
            )
        }.map { page -> page.map(PostRecord::toPost) }

    private suspend fun fetchFeed(feed: Feed, viewer: String, until: Long?): FetchedPage {
        val authors = when (feed) {
            Feed.Latest -> null
            Feed.Following -> {
                if (viewer.isEmpty()) return FetchedPage.NOTHING
                sync.ensureListsSynced(viewer)
                follows.observeFollowees(viewer).first() + viewer
            }
            is Feed.UserPosts -> listOf(feed.userId.value)
            is Feed.UserReplies -> listOf(feed.userId.value)
        }
        val page = postsRemote.timeline(authors, until, FeedRules.PAGE_SIZE)
        val newItems = sync.storeWithContext(page)
        // Following also shows what they reposted: the reposts, and the posts they point at.
        val reposts = if (feed == Feed.Following && authors != null) postsRemote.reposts(authors, until, FeedRules.PAGE_SIZE) else emptyList()
        if (reposts.isNotEmpty()) {
            val stored = reposts.map { it.postId }.distinct().filter { posts.authorOf(it) == null }
            sync.storeWithContext(postsRemote.byIds(stored))
            ingestor.ingest(reposts)
        }
        val oldest = (page.map { it.event.createdAt } + reposts.map { it.event.createdAt }).minOrNull()
        return FetchedPage(oldest, page.size, newItems)
    }

    override fun observePost(id: PostId): Flow<Post?> =
        session.viewer.flatMapLatest { viewer -> posts.observePost(id.value, viewer) }.map { it?.toPost() }

    override suspend fun refreshPost(id: PostId): Result<Unit> = remote {
        sync.storeWithContext(postsRemote.byIds(listOf(id.value)))
    }

    override fun observeThread(id: PostId): Flow<List<ThreadReply>> =
        session.viewer.flatMapLatest { viewer -> posts.observeThread(id.value, viewer) }
            .map { records -> records.map { ThreadReply(it.toPost(), it.depth) } }

    override suspend fun refreshThread(id: PostId): Result<Unit> = remote {
        val post = requirePost(id)
        // NIP-10 replies tag the thread root, so one query returns the whole thread.
        sync.storeWithContext(postsRemote.thread(post.rootId ?: post.id))
    }

    override fun observeRevisions(id: PostId): Flow<List<Revision>> =
        posts.observeRevisions(id.value).map { revisions -> revisions.map { it.toRevision() } }

    override suspend fun refreshRevisions(id: PostId): Result<Unit> = remote {
        ingestor.ingest(postsRemote.related(listOf(id.value)))
    }

    override suspend fun publish(content: String, images: List<Image>, quoteOf: PostId?): Result<PostId> = remote {
        val quote = quoteOf?.let { PostRef(it.value, posts.authorOf(it.value).orEmpty(), editedRevisionId(it.value)) }
        val event = publisher.sign { TalkEvents.post(content, images.toImeta(), quote, it) }
        publisher.storeAndPublish(event)
        PostId(event.id)
    }

    override suspend fun reply(parent: PostId, content: String, images: List<Image>): Result<PostId> = remote {
        val parentPost = requirePost(parent)
        val root = parentPost.rootId?.let { PostRef(it, posts.authorOf(it).orEmpty()) }
        val parentRef = PostRef(parentPost.id, parentPost.author, editedRevisionId(parentPost.id))
        val event = publisher.sign { TalkEvents.reply(content, images.toImeta(), parentRef, root, it) }
        publisher.storeAndPublish(event)
        PostId(event.id)
    }

    override suspend fun retryPublish(id: PostId): Result<Unit> = remote {
        val event = posts.eventJson(id.value)?.let(Event::fromJson) ?: throw DomainError.NotFound
        if (event.pubkey != publisher.me()) throw DomainError.NotFound
        posts.setPublishState(event.id, PublishStates.PENDING) // "Sending…" again
        publisher.storeAndPublish(event)
    }

    override suspend fun edit(id: PostId, content: String, images: List<Image>): Result<Unit> = remote {
        val post = requireOwnPost(id)
        // Number after the newest revision relays know of, not only the ones cached here.
        runCatching { ingestor.ingest(postsRemote.related(listOf(id.value))) }
        val number = posts.observeRevisions(id.value).first().maxOfOrNull { it.number }?.plus(1) ?: (post.revision + 1)
        val event = publisher.sign { TalkEvents.revision(id.value, number, content, images.toImeta(), it) }
        publisher.storeAndPublish(event)
    }

    override suspend fun delete(id: PostId): Result<Unit> = remote {
        val post = requireOwnPost(id)
        val revisions = posts.observeRevisions(id.value).first().filter { it.number > 1 }.associate { it.id to TalkSchema.KIND_REVISION }
        val event = publisher.sign { TalkEvents.delete(mapOf(post.id to Kinds.TEXT_NOTE) + revisions, it) }
        // Deletions are not optimistic: tombstones cannot be undone if no relay takes the request.
        publisher.publish(event)
        posts.markDeleted(listOf(post.id) + revisions.keys, author = post.author)
    }

    private suspend fun requirePost(id: PostId): PostRecord {
        posts.observePost(id.value, "").first()?.let { return it }
        sync.storeWithContext(postsRemote.byIds(listOf(id.value)))
        return posts.observePost(id.value, "").first() ?: throw DomainError.NotFound
    }

    private suspend fun requireOwnPost(id: PostId): PostRecord {
        val post = requirePost(id)
        if (post.author != publisher.me()) throw DomainError.NotFound
        return post
    }

    /** The revision event id to pin when the post was edited; null for an unedited post. */
    private suspend fun editedRevisionId(postId: String): String? =
        posts.observeRevisions(postId).first().firstOrNull()?.takeIf { it.number > 1 }?.id

    private fun Feed.key(viewer: String): String = when (this) {
        Feed.Latest -> FeedKeys.LATEST
        Feed.Following -> FeedKeys.following(viewer)
        is Feed.UserPosts -> FeedKeys.posts(userId.value)
        is Feed.UserReplies -> FeedKeys.replies(userId.value)
    }

    private fun Feed.toDao(): PostFeed = when (this) {
        Feed.Latest -> PostFeed.Latest
        Feed.Following -> PostFeed.Following
        is Feed.UserPosts -> PostFeed.ByAuthor(userId.value, replies = false)
        is Feed.UserReplies -> PostFeed.ByAuthor(userId.value, replies = true)
    }
}
