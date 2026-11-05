package com.yet.talk.feature.settings

import com.app.common.decompose.asValue
import com.arkivanov.decompose.ComponentContext
import com.yet.talk.domain.model.settings.ThemeMode
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.yet.talk.feature.settings.mapper.stateToModel
import com.yet.talk.feature.settings.store.SettingsStore
import com.yet.talk.feature.settings.store.SettingsStoreFactory
import dev.zacsweers.metro.Inject

internal class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
    storeFactory: SettingsStoreFactory,
) : SettingsComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create() }

    override val model: Value<SettingsComponent.Model> = store.asValue().map(stateToModel)

    override fun onBack() = onBack.invoke()

    override fun onRelayInputChanged(url: String) = store.accept(SettingsStore.Intent.ChangeRelayInput(url))

    override fun onAddRelay() = store.accept(SettingsStore.Intent.AddRelay)

    override fun onRemoveRelay(url: String) = store.accept(SettingsStore.Intent.RemoveRelay(url))

    override fun onExportKey() = store.accept(SettingsStore.Intent.ExportKey)

    override fun onThemeSelected(theme: ThemeMode) = store.accept(SettingsStore.Intent.SelectTheme(theme))

    override fun onHideKey() = store.accept(SettingsStore.Intent.HideKey)

    override fun onSignOut() = store.accept(SettingsStore.Intent.SignOut)

    override fun onErrorShown() = store.accept(SettingsStore.Intent.DismissError)
}

@Inject
internal class DefaultSettingsComponentFactory(private val storeFactory: SettingsStoreFactory) : SettingsComponent.Factory {
    override fun invoke(componentContext: ComponentContext, onBack: () -> Unit): SettingsComponent =
        DefaultSettingsComponent(componentContext, onBack, storeFactory)
}
