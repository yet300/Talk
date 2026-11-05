package com.yet.talk.feature.auth.mapper

import com.yet.talk.feature.auth.AuthComponent
import com.yet.talk.feature.auth.store.AuthStore

internal val stateToModel: (AuthStore.State) -> AuthComponent.Model = { state ->
    AuthComponent.Model(
        key = state.key,
        isWorking = state.isWorking,
        canImport = state.key.isNotBlank() && !state.isWorking,
        error = state.error,
    )
}
