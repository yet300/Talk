package com.yet.talk.database.dao

import com.yet.talk.database.model.RelayRecord
import kotlinx.coroutines.flow.Flow

/** NIP-65 relay lists. */
interface RelayListDao {
    /** Applies only if [createdAt] is newer than the stored list. */
    suspend fun replace(owner: String, relays: List<RelayRecord>, createdAt: Long): Boolean

    suspend fun version(owner: String): Long?

    fun observe(owner: String): Flow<List<RelayRecord>>
}
