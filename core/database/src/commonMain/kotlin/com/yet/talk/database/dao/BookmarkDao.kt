package com.yet.talk.database.dao

/** NIP-51 bookmark lists. The bookmarked posts are read through [PostDao.feedPages]. */
interface BookmarkDao {
    /** Applies only if [createdAt] is newer than the stored list. */
    suspend fun replace(owner: String, postIds: List<String>, createdAt: Long): Boolean

    suspend fun version(owner: String): Long?

    suspend fun bookmarkIds(owner: String): List<String>
}
