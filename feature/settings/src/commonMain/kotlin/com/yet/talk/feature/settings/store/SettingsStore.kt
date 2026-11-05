package com.yet.talk.feature.settings.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.relay.Relay

internal interface SettingsStore : Store<SettingsStore.Intent, SettingsStore.State, Nothing> {

    data class State(
        val account: Account? = null,
        val relays: List<Relay> = emptyList(),
        val relayInput: String = "",
        val exportedKey: String? = null,
        val isWorking: Boolean = false,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class ChangeRelayInput(val url: String) : Intent

        data object AddRelay : Intent

        data class RemoveRelay(val url: String) : Intent

        data object ExportKey : Intent

        data object HideKey : Intent

        data object SignOut : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object Observe : Action
    }

    sealed interface Msg {
        data class AccountChanged(val account: Account?) : Msg

        data class RelaysChanged(val relays: List<Relay>) : Msg

        data class RelayInputChanged(val url: String) : Msg

        data class KeyExported(val key: String?) : Msg

        data class Working(val working: Boolean) : Msg

        data class Failed(val error: DomainError?) : Msg
    }
}
