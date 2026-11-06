package com.yet.talk.domain.usecase.profile

import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.ProfileEdit
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.domain.rule.ProfileRules
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

@Inject
class UpdateProfile(private val profiles: ProfileRepository, private val accounts: AccountRepository) {
    suspend operator fun invoke(edit: ProfileEdit): Result<Unit> {
        val me = accounts.account.first()?.userId ?: return Result.failure(DomainError.NotSignedIn)
        val hasVerifiedGitHub = profiles.observeUser(me).first().github != null
        return ProfileRules.validate(edit, hasVerifiedGitHub).fold(onSuccess = { profiles.updateMyProfile(it) }, onFailure = { Result.failure(it) })
    }
}
