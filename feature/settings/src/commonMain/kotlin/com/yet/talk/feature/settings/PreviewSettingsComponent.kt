package com.yet.talk.feature.settings

import com.arkivanov.decompose.value.MutableValue
import com.yet.talk.domain.model.settings.ThemeMode
import com.arkivanov.decompose.value.Value

/** For UI previews: a fixed model, actions do nothing. */
class PreviewSettingsComponent(
    model: SettingsComponent.Model = SettingsComponent.Model(
        account = null, me = null, theme = ThemeMode.System, relays = emptyList(), relayInput = "", exportedKey = null, isWorking = false, error = null,
    ),
) : SettingsComponent {
    override val model: Value<SettingsComponent.Model> = MutableValue(model)

    override fun onBack() = Unit

    override fun onRelayInputChanged(url: String) = Unit

    override fun onAddRelay() = Unit

    override fun onRemoveRelay(url: String) = Unit

    override fun onExportKey() = Unit

    override fun onThemeSelected(theme: ThemeMode) = Unit

    override fun onHideKey() = Unit

    override fun onSignOut() = Unit

    override fun onErrorShown() = Unit
}
