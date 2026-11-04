package com.yet.talk.database.dao

import com.yet.talk.database.model.ActivityRecord
import com.yet.talk.database.model.RelayRecord
import kotlinx.coroutines.flow.Flow

/** Replaceable lists (NIP-02 contacts, NIP-51 bookmarks, NIP-65 relays); each replace applies only if [createdAt] is newer. */
interface SocialDao {
    suspend fun replaceFollows(follower: String, followees: List<String>, createdAt: Long): Boolean

    fun observeFollowees(follower: String): Flow<List<String>>

    fun observeIsFollowing(follower: String, followee: String): Flow<Boolean>

    fun observeFollowingCount(follower: String): Flow<Long>

    suspend fun replaceBookmarks(owner: String, postIds: List<String>, createdAt: Long): Boolean

    suspend fun bookmarkIds(owner: String): List<String>

    suspend fun replaceRelays(owner: String, relays: List<RelayRecord>, createdAt: Long): Boolean

    fun observeRelays(owner: String): Flow<List<RelayRecord>>

    /** created_at of the stored list of [kind] by [pubkey]; null when none is stored. */
    suspend fun listVersion(pubkey: String, kind: Int): Long?

    fun observeActivity(me: String, limit: Int): Flow<List<ActivityRecord>>
}
