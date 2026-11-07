package com.yet.talk.database.dao

import kotlinx.coroutines.flow.Flow

interface ReportDao {
    suspend fun insert(eventId: String, reporter: String, targetPubkey: String, targetEvent: String?, type: String, createdAt: Long)

    /** Ids of posts reported by [viewer] or by someone they follow. */
    fun observeHiddenPosts(viewer: String): Flow<List<String>>
}
