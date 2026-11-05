package com.yet.talk.feature.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.relay.Relay

/** Identity, relays and sign-out. */
interface SettingsComponent {
    val model: Value<Model>

    fun onBack()

    fun onRelayInputChanged(url: String)

    fun onAddRelay()

    fun onRemoveRelay(url: String)

    /** Reveals the `nsec` for the user's own backup, until [onHideKey]. */
    fun onExportKey()

    fun onHideKey()

    fun onSignOut()

    fun onErrorShown()

    data class Model(
        val account: Account?,
        val relays: List<Relay>,
        val relayInput: String,
        val exportedKey: String?,
        val isWorking: Boolean,
        val error: DomainError?,
    )

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, onBack: () -> Unit): SettingsComponent
    }
}
