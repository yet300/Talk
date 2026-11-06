package com.yet.talk.domain.usecase.profile

import com.yet.talk.domain.model.user.ProfileEdit
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.domain.rule.ProfileRules
import dev.zacsweers.metro.Inject

@Inject
class UpdateProfile(private val profiles: ProfileRepository) {
    suspend operator fun invoke(edit: ProfileEdit): Result<Unit> =
        ProfileRules.validate(edit).fold(onSuccess = { profiles.updateMyProfile(it) }, onFailure = { Result.failure(it) })
}
