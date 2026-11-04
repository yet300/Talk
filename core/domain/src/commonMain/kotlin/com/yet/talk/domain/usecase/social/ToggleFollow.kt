package com.yet.talk.domain.usecase.social

import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.FollowRepository
import dev.zacsweers.metro.Inject

@Inject
class ToggleFollow(private val follows: FollowRepository) {
    suspend operator fun invoke(user: UserId, currentlyFollowing: Boolean): Result<Unit> =
        follows.setFollowing(user, !currentlyFollowing)
}
