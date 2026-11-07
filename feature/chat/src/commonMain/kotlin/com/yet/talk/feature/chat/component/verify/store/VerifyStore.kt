package com.yet.talk.feature.chat.component.verify.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.chat.SafetyCode
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import kotlin.time.Instant

internal interface VerifyStore : Store<VerifyStore.Intent, VerifyStore.State, Nothing> {

    data class State(
        val person: UserId,
        val user: User? = null,
        val code: SafetyCode? = null,
        val verifiedAt: Instant? = null,
        val scanMismatch: Boolean = false,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class SetVerified(val verified: Boolean) : Intent

        data class Scanned(val payload: String) : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object Load : Action
    }

    sealed interface Msg {
        data class UserChanged(val user: User) : Msg

        data class CodeLoaded(val code: SafetyCode) : Msg

        data class VerifiedChanged(val at: Instant?) : Msg

        data class ScanMismatch(val mismatch: Boolean) : Msg

        data class Failed(val error: DomainError?) : Msg
    }
}
