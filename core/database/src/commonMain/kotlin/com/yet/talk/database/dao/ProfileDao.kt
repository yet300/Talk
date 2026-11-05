package com.yet.talk.database.dao

import com.yet.talk.database.model.ProfileRecord
import kotlinx.coroutines.flow.Flow

interface ProfileDao {
    /** Keeps the newer of the stored and the given profile. */
    suspend fun upsert(profile: ProfileRecord)

    fun observe(pubkey: String): Flow<ProfileRecord?>

    /** Profiles whose name, display name or NIP-05 contains [text]. */
    fun observeSearch(text: String, limit: Int): Flow<List<ProfileRecord>>
}
