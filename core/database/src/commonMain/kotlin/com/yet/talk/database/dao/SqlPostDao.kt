package com.yet.talk.database.dao

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@ContributesBinding(AppScope::class)
@Inject
class SqlPostDao(
    private val holder: DatabaseHolder,
    private val dispatchers: AppDispatchers,
) : PostDao {

    override suspend fun insert(post: NewPostRecord): Boolean = withContext(dispatchers.io) {
        val database = holder.get()
        database.transactionWithResult {
            if (database.socialQueries.isTombstoned(post.id).awaitAsOne()) return@transactionWithResult false
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

    override suspend fun markDeleted(ids: Collection<String>) = withContext(dispatchers.io) {
        if (ids.isEmpty()) return@withContext
        val database = holder.get()
        database.transaction {
            database.postQueries.markDeleted(ids)
            ids.forEach { database.socialQueries.insertTombstone(it) }
        }
    }

    override suspend fun eventJson(id: String): String? = withContext(dispatchers.io) {
        holder.get().postQueries.selectEventJson(id).awaitAsOneOrNull()
    }

    override fun observePost(id: String, viewer: String): Flow<PostRecord?> =
        holder.observeOneOrNull(dispatchers.io) { postQueries.selectPost(viewer, id, ::postRecord) }

    override fun observeFeed(feed: PostFeed, viewer: String, limit: Int): Flow<List<PostRecord>> {
        val count = limit.toLong()
        return holder.observeList(dispatchers.io) {
            when (feed) {
                PostFeed.Latest -> postQueries.selectLatest(viewer, count, ::postRecord)
                PostFeed.Following -> postQueries.selectFollowing(viewer, count, ::postRecord)
                is PostFeed.ByAuthor ->
                    if (feed.replies) postQueries.selectUserReplies(viewer, feed.author, count, ::postRecord)
                    else postQueries.selectUserPosts(viewer, feed.author, count, ::postRecord)
                PostFeed.Bookmarks -> postQueries.selectBookmarked(viewer, count, ::postRecord)
                PostFeed.Trending -> postQueries.selectTrending(viewer, count, ::postRecord)
            }
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

@Suppress("UNUSED_PARAMETER") // `deleted` is filtered in SQL; every post query shares this column list.
internal fun postRecord(
    id: String, author: String, rootId: String?, parentId: String?, quoteId: String?, createdAt: Long, content: String,
    imagesJson: String, revision: Long, revisionCount: Long, editedAt: Long?, deleted: Long, publishState: String,
    authorName: String?, authorDisplayName: String?, authorPicture: String?, authorNip05: String?,
    likeCount: Long, repostCount: Long, replyCount: Long, viewerLiked: Boolean, viewerReposted: Boolean, viewerBookmarked: Boolean,
) = PostRecord(
    id = id, author = author, rootId = rootId, parentId = parentId, quoteId = quoteId, createdAt = createdAt,
    content = content, imagesJson = imagesJson, revision = revision.toInt(), revisionCount = revisionCount.toInt(),
    editedAt = editedAt, publishState = publishState, authorName = authorName, authorDisplayName = authorDisplayName,
    authorPicture = authorPicture, authorNip05 = authorNip05, likeCount = likeCount, repostCount = repostCount,
    replyCount = replyCount, viewerLiked = viewerLiked, viewerReposted = viewerReposted, viewerBookmarked = viewerBookmarked,
)
