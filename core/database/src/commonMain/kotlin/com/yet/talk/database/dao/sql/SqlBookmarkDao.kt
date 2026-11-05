package com.yet.talk.database.dao.sql

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.BookmarkDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext

@Inject
internal class SqlBookmarkDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : BookmarkDao {

    override suspend fun replace(owner: String, postIds: List<String>, createdAt: Long): Boolean = withContext(dispatchers.io) {
        holder.get().replaceList(owner, KIND_BOOKMARKS, createdAt) {
            bookmarkQueries.deleteBookmarks(owner)
            postIds.distinct().forEachIndexed { position, id -> bookmarkQueries.insertBookmark(owner, id, position.toLong()) }
        }
    }

    override suspend fun version(owner: String): Long? = withContext(dispatchers.io) {
        holder.get().listVersion(owner, KIND_BOOKMARKS)
    }

    override suspend fun bookmarkIds(owner: String): List<String> = withContext(dispatchers.io) {
        holder.get().bookmarkQueries.selectBookmarkIds(owner).awaitAsList()
    }
}
