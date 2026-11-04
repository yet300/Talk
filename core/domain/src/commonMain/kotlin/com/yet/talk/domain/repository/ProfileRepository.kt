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

    suspend fun updateMyProfile(edit: ProfileEdit): Result<Unit>
}
