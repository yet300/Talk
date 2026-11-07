package com.yet.talk.feature.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.account.AppLock
import com.yet.talk.domain.model.network.RelayState
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.relay.Relay
import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.domain.model.user.User

/** Identity, relays, Security (app lock, erase chats, erase everything) and sign-out. */
interface SettingsComponent {
    val model: Value<Model>

    fun onBack()

    fun onEditProfile()

    fun onRelayInputChanged(url: String)

    fun onAddRelay()

    fun onRemoveRelay(url: String)

    /** Reveals the `nsec` for the user's own backup, until [onHideKey]. */
    fun onExportKey()

    fun onThemeSelected(theme: ThemeMode)

    fun onHideKey()

    fun onSignOut()

    /** Turns the passcode lock on (web); [confirm] must repeat [passcode]. */
    fun onSetPasscode(passcode: String, confirm: String)

    fun onRemovePasscode()

    /** Erase chats from this device (after the UI's confirmation). */
    fun onEraseChats()

    /** Erase everything: the account key too, as Sign out does (after the UI's backup warning). */
    fun onEraseEverything()

    fun onErrorShown()

    data class Model(
        val account: Account?,
        /** The signed-in user's profile (name, picture); null until it is known. */
        val me: User?,
        val theme: ThemeMode,
        val relays: List<Relay>,
        /** Whether each relay is connected now. */
        val relayStates: Map<String, RelayState>,
        val relayInput: String,
        val exportedKey: String?,
        val lock: AppLock,
        val isWorking: Boolean,
        val error: DomainError?,
    )

    sealed interface Output {
        data object Back : Output

        data object EditProfile : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, output: (Output) -> Unit): SettingsComponent
    }
}
