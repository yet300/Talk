package com.yet.talk.database.dao

import com.yet.talk.database.model.SyncStateRecord

/** Remote paging state per feed. */
interface SyncDao {
    suspend fun state(feedKey: String): SyncStateRecord?

    suspend fun setState(feedKey: String, state: SyncStateRecord)

    /** Forgets the feed's remote cursor, so paging starts again from the newest events. */
    suspend fun reset(feedKey: String)
}
