package com.yet.talk.feature.settings.mapper

import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.feature.settings.store.SettingsStore

internal val stateToModel: (SettingsStore.State) -> SettingsComponent.Model = { state ->
    SettingsComponent.Model(
        account = state.account,
        relays = state.relays,
        relayInput = state.relayInput,
        exportedKey = state.exportedKey,
        isWorking = state.isWorking,
        error = state.error,
    )
}
