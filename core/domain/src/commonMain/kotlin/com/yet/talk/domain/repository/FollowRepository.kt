package com.yet.talk.domain.repository

import com.yet.talk.domain.model.user.UserId
import kotlinx.coroutines.flow.Flow

interface FollowRepository {
    fun observeIsFollowing(id: UserId): Flow<Boolean>

    suspend fun setFollowing(id: UserId, following: Boolean): Result<Unit>
}
