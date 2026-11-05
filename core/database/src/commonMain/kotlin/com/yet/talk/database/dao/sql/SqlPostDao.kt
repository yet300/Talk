package com.yet.talk.database.dao.sql

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import androidx.paging.PagingSource
import app.cash.sqldelight.paging3.QueryPagingSource
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.mapper.postRecord
import com.yet.talk.database.model.NewPostRecord
import com.yet.talk.database.model.PostFeed
import com.yet.talk.database.model.PostRecord
import com.yet.talk.database.model.RevisionRecord
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
internal class SqlPostDao(
    private val holder: DatabaseHolder,
    private val dispatchers: AppDispatchers,
) : PostDao {

    override suspend fun insert(post: NewPostRecord): Boolean = withContext(dispatchers.io) {
        val database = holder.get()
        database.transactionWithResult {
            if (database.tombstoneQueries.isTombstoned(post.id, post.author).awaitAsOne()) return@transactionWithResult false
            with(post) {
                database.postQueries.insertIfAbsent(id, author, rootId, parentId, quoteId, createdAt, content, imagesJson, publishState, eventJson)
            } > 0
        }
    }

    override suspend fun addRevision(revision: RevisionRecord) = withContext(dispatchers.io) {
        val database = holder.get()
        database.transaction {
            with(revision) { database.postQueries.insertRevision(id, postId, number.toLong(), content, imagesJson, createdAt) }
            database.postQueries.applyLatestRevision(revision.postId)
        }
    }

    override suspend fun setPublishState(id: String, state: String) = withContext(dispatchers.io) {
        holder.get().postQueries.setPublishState(state, id)
        Unit
    }

    override suspend fun markDeleted(ids: Collection<String>, author: String) = withContext(dispatchers.io) {
        if (ids.isEmpty()) return@withContext
        val database = holder.get()
        database.transaction {
            database.postQueries.markDeleted(ids, author)
            ids.forEach { database.tombstoneQueries.insertTombstone(it, author) }
        }
    }

    override suspend fun authorOf(id: String): String? = withContext(dispatchers.io) {
        holder.get().postQueries.selectAuthor(id).awaitAsOneOrNull()
    }

    override suspend fun eventJson(id: String): String? = withContext(dispatchers.io) {
        holder.get().postQueries.selectEventJson(id).awaitAsOneOrNull()
    }

    override fun observePost(id: String, viewer: String): Flow<PostRecord?> =
        holder.observeOneOrNull(dispatchers.io) { postQueries.selectPost(viewer, id, ::postRecord) }

    override suspend fun feedPages(feed: PostFeed, viewer: String): () -> PagingSource<Int, PostRecord> {
        val queries = holder.get().postQueries
        val count = when (feed) {
            PostFeed.Latest -> queries.countLatest()
            PostFeed.Following -> queries.countFollowing(viewer)
            is PostFeed.ByAuthor -> if (feed.replies) queries.countUserReplies(feed.author) else queries.countUserPosts(feed.author)
            PostFeed.Bookmarks -> queries.countBookmarked(viewer)
            PostFeed.Trending -> queries.countTrending()
        }
        return {
            QueryPagingSource(countQuery = count, transacter = queries, context = dispatchers.io, queryProvider = { limit: Long, offset: Long ->
                when (feed) {
                    PostFeed.Latest -> queries.selectLatest(viewer, limit, offset, ::postRecord)
                    PostFeed.Following -> queries.selectFollowing(viewer, limit, offset, ::postRecord)
                    is PostFeed.ByAuthor ->
                        if (feed.replies) queries.selectUserReplies(viewer, feed.author, limit, offset, ::postRecord)
                        else queries.selectUserPosts(viewer, feed.author, limit, offset, ::postRecord)
                    PostFeed.Bookmarks -> queries.selectBookmarked(viewer, limit, offset, ::postRecord)
                    PostFeed.Trending -> queries.selectTrending(viewer, limit, offset, ::postRecord)
                }
            })
        }
    }

    override fun observeThread(rootId: String, viewer: String): Flow<List<PostRecord>> =
        holder.observeList(dispatchers.io) {
            postQueries.selectThread(rootId, viewer) { id, author, root, parent, quote, createdAt, content, images, revision,
                revisionCount, editedAt, deleted, publishState, name, displayName, picture, nip05, likes, reposts, replies,
                liked, reposted, bookmarked, depth ->
                postRecord(
                    id, author, root, parent, quote, createdAt, content, images, revision, revisionCount, editedAt, deleted,
                    publishState, name, displayName, picture, nip05, likes, reposts, replies, liked, reposted, bookmarked,
                ).copy(depth = depth.toInt())
            }
        }

    override fun observeRevisions(postId: String): Flow<List<RevisionRecord>> =
        holder.observeList(dispatchers.io) {
            postQueries.selectRevisions(postId) { id, post, number, content, images, createdAt ->
                RevisionRecord(id, post, number.toInt(), content, images, createdAt)
            }
        }
}
