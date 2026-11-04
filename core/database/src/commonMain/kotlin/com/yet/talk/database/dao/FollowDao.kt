package com.yet.talk.database.dao

import kotlinx.coroutines.flow.Flow

/** NIP-02 contact lists. */
interface FollowDao {
    /** Applies only if [createdAt] is newer than the stored list. */
    suspend fun replace(follower: String, followees: List<String>, createdAt: Long): Boolean

    /** created_at of [follower]'s stored list; null when none is stored. */
    suspend fun version(follower: String): Long?

    fun observeFollowees(follower: String): Flow<List<String>>

    fun observeIsFollowing(follower: String, followee: String): Flow<Boolean>

    fun observeFollowingCount(follower: String): Flow<Long>
}
