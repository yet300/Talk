package com.yet.talk.feature.settings.mapper

import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.feature.settings.store.SettingsStore

internal val stateToModel: (SettingsStore.State) -> SettingsComponent.Model = { state ->
    SettingsComponent.Model(
        account = state.account,
        me = state.me,
        theme = state.theme,
        relays = state.relays,
        relayInput = state.relayInput,
        exportedKey = state.exportedKey,
        lock = state.lock,
        isWorking = state.isWorking,
        error = state.error,
    )
}
