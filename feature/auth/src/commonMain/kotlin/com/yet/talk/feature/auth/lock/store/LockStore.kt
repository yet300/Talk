package com.yet.talk.feature.auth.lock.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.error.DomainError

internal interface LockStore : Store<LockStore.Intent, LockStore.State, Nothing> {

    data class State(val passcode: String = "", val isUnlocking: Boolean = false, val error: DomainError? = null)

    sealed interface Intent {
        data class ChangePasscode(val passcode: String) : Intent

        data object Unlock : Intent

        data object Forgot : Intent

        data object DismissError : Intent
    }

    sealed interface Msg {
        data class PasscodeChanged(val passcode: String) : Msg

        data class Unlocking(val unlocking: Boolean) : Msg

        data class Failed(val error: DomainError?) : Msg
    }
}
