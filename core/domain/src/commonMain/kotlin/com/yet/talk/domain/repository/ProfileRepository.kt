package com.yet.talk.domain.repository

import com.yet.talk.domain.model.user.ProfileEdit
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.model.user.UserStats
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    /** A placeholder user (id and npub only) until the profile is known. */
    fun observeUser(id: UserId): Flow<User>

    fun observeStats(id: UserId): Flow<UserStats>

    suspend fun refresh(id: UserId): Result<Unit>

    /** Publishes a new kind 0 that keeps the fields [edit] does not cover. */
    suspend fun updateMyProfile(edit: ProfileEdit): Result<Unit>

    /** True when the NIP-05 [identifier] (`name@domain`) points at [id]. */
    suspend fun verifyNip05(id: UserId, identifier: String): Result<Boolean>
}
