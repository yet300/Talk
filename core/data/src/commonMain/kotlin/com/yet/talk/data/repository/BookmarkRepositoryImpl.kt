package com.yet.talk.data.repository

import com.yet.talk.api.source.post.PostRemoteSource
import com.yet.talk.data.mapper.toPost
import com.yet.talk.data.paging.FeedPager
import com.yet.talk.data.paging.FetchedPage
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.data.sync.publishReplaceable
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.dao.SocialDao
import com.yet.talk.database.model.PostFeed
import com.yet.talk.database.model.PostRecord
import com.yet.talk.domain.model.Pager
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.repository.BookmarkRepository
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.bookmarkList
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

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

    private suspend fun publishList(me: String, kind: Int, build: (Long) -> EventTemplate) =
        publishReplaceable(social, ingestor, publisher, me, kind, build)
}
