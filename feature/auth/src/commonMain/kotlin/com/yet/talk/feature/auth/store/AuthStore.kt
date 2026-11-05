package com.yet.talk.feature.auth.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.error.DomainError

internal interface AuthStore : Store<AuthStore.Intent, AuthStore.State, Nothing> {

    data class State(
        val key: String = "",
        /** A key just created, shown once ("Save your key") until the user confirms they saved it. */
        val newKey: String? = null,
        val newKeyCopied: Boolean = false,
        /** "I saved it" unlocks after Copy or after a short wait. */
        val newKeySaveReady: Boolean = false,
        val isWorking: Boolean = false,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class ChangeKey(val key: String) : Intent

        data object ImportKey : Intent

        data object CreateKey : Intent

        data object NewKeyCopied : Intent

        data object NewKeySaved : Intent

        data object DismissError : Intent
    }

    sealed interface Msg {
        data class KeyChanged(val key: String) : Msg

        data object Started : Msg

        data class KeyCreated(val nsec: String) : Msg

        data object NewKeyCopied : Msg

        data object SaveReady : Msg

        data class Finished(val error: DomainError?) : Msg

        data object ErrorDismissed : Msg
    }
}
