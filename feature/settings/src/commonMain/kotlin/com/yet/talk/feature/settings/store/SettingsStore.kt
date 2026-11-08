package com.yet.talk.feature.settings.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.account.AppLock
import com.yet.talk.domain.model.network.RelayState
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.relay.Relay
import com.yet.talk.domain.model.settings.Privacy
import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.domain.model.user.User

internal interface SettingsStore : Store<SettingsStore.Intent, SettingsStore.State, Nothing> {

    data class State(
        val account: Account? = null,
        /** The signed-in user's profile, for the Identity row. */
        val me: User? = null,
        val relays: List<Relay> = emptyList(),
        val relayStates: Map<String, RelayState> = emptyMap(),
        val relayInput: String = "",
        val exportedKey: String? = null,
        val lock: AppLock = AppLock(),
        val privacy: Privacy = Privacy(),
        val theme: ThemeMode = ThemeMode.System,
        val isWorking: Boolean = false,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class ChangeRelayInput(val url: String) : Intent

        data object AddRelay : Intent

        data class RemoveRelay(val url: String) : Intent

        data object ExportKey : Intent

        data class SelectTheme(val theme: ThemeMode) : Intent

        data object HideKey : Intent

        data object SignOut : Intent

        data class SetPasscode(val passcode: String, val confirm: String) : Intent

        data object RemovePasscode : Intent

        data class SetDeviceLock(val enabled: Boolean) : Intent

        data class SetHideInScreenshots(val hidden: Boolean) : Intent

        data object EraseChats : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object Observe : Action
    }

    sealed interface Msg {
        data class AccountChanged(val account: Account?) : Msg

        data class RelaysChanged(val relays: List<Relay>) : Msg

        data class MeChanged(val me: User?) : Msg

        data class ThemeChanged(val theme: ThemeMode) : Msg

        data class LockChanged(val lock: AppLock) : Msg

        data class PrivacyChanged(val privacy: Privacy) : Msg

        data class RelayStatesChanged(val states: Map<String, RelayState>) : Msg

        data class RelayInputChanged(val url: String) : Msg

        data class KeyExported(val key: String?) : Msg

        data class Working(val working: Boolean) : Msg

        data class Failed(val error: DomainError?) : Msg
    }
}
