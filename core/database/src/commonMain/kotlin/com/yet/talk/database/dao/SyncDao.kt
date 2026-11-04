package com.yet.talk.database.dao

import com.yet.talk.database.model.SyncStateRecord

interface SyncDao {
    suspend fun state(feedKey: String): SyncStateRecord?

    suspend fun setState(feedKey: String, state: SyncStateRecord)
}
