package com.yet.talk.feature.settings

import com.arkivanov.decompose.value.MutableValue
import com.yet.talk.domain.model.account.AppLock
import com.yet.talk.domain.model.settings.ThemeMode
import com.arkivanov.decompose.value.Value

/** For UI previews: a fixed model, actions do nothing. */
class PreviewSettingsComponent(
    model: SettingsComponent.Model = SettingsComponent.Model(
        account = null, me = null, theme = ThemeMode.System, relays = emptyList(), relayStates = emptyMap(), relayInput = "", exportedKey = null, lock = AppLock(), isWorking = false, error = null,
    ),
) : SettingsComponent {
    override val model: Value<SettingsComponent.Model> = MutableValue(model)

    override fun onBack() = Unit

    override fun onEditProfile() = Unit

    override fun onRelayInputChanged(url: String) = Unit

    override fun onAddRelay() = Unit

    override fun onRemoveRelay(url: String) = Unit

    override fun onExportKey() = Unit

    override fun onThemeSelected(theme: ThemeMode) = Unit

    override fun onHideKey() = Unit

    override fun onSignOut() = Unit

    override fun onSetPasscode(passcode: String, confirm: String) = Unit

    override fun onRemovePasscode() = Unit

    override fun onEraseChats() = Unit

    override fun onEraseEverything() = Unit

    override fun onErrorShown() = Unit
}
