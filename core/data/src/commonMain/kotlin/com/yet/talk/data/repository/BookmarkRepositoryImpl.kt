package com.yet.talk.data.repository

import androidx.paging.PagingData
import androidx.paging.map
import com.yet.talk.api.source.post.PostRemoteSource
import com.yet.talk.data.mapper.toPost
import com.yet.talk.data.paging.FeedKeys
import com.yet.talk.data.paging.FeedPaging
import com.yet.talk.data.paging.FetchedPage
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.data.sync.publishReplaceable
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.BookmarkDao
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.model.PostFeed
import com.yet.talk.database.model.PostRecord
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.repository.BookmarkRepository
import com.yet.talk.nostr.nip.bookmarkList
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
internal class BookmarkRepositoryImpl(
    private val posts: PostDao,
    private val bookmarks: BookmarkDao,
    private val postsRemote: PostRemoteSource,
    private val sync: RemoteSync,
    private val ingestor: EventIngestor,
    private val session: Session,
    private val publisher: Publisher,
    private val paging: FeedPaging,
) : BookmarkRepository {

    override suspend fun setBookmarked(postId: PostId, bookmarked: Boolean): Result<Unit> = remote {
        val me = publisher.me()
        sync.ensureListsSynced(me)
        val current = bookmarks.bookmarkIds(me)
        val updated = if (bookmarked) current + postId.value else current - postId.value
        if (updated.distinct() == current) return@remote
        publishReplaceable(bookmarks.version(me), ingestor, publisher) { bookmarkList(updated, it) }
    }

    // The whole list arrives at once: refresh fetches every bookmarked post, there are no older pages.
    override fun bookmarks(): Flow<PagingData<Post>> =
        session.viewer.flatMapLatest { viewer ->
            paging.feed(
                feedKey = FeedKeys.bookmarks(viewer),
                pages = { posts.feedPages(PostFeed.Bookmarks, viewer) },
                fetcher = { until -> if (until == null && viewer.isNotEmpty()) FetchedPage(fetchAll(viewer)) else FetchedPage.NOTHING },
            )
        }.map { page -> page.map(PostRecord::toPost) }

    private suspend fun fetchAll(viewer: String): Long {
        sync.syncLists(viewer)
        sync.storeWithContext(postsRemote.byIds(bookmarks.bookmarkIds(viewer)))
        return publisher.now()
    }
}
