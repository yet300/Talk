package com.yet.talk.feature.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.relay.Relay
import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.domain.model.user.User

/** Identity, relays and sign-out. */
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

    fun onErrorShown()

    data class Model(
        val account: Account?,
        /** The signed-in user's profile (name, picture); null until it is known. */
        val me: User?,
        val theme: ThemeMode,
        val relays: List<Relay>,
        val relayInput: String,
        val exportedKey: String?,
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
