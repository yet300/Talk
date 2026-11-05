package com.yet.talk.feature.root.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.settings.ThemeMode

/** Whether someone is signed in (null until the saved key has been read), and the app theme. */
internal interface RootStore : Store<Nothing, RootStore.State, Nothing> {

    data class State(val signedIn: Boolean? = null, val theme: ThemeMode = ThemeMode.System)

    sealed interface Action {
        data object ObserveAccount : Action
    }

    sealed interface Msg {
        data class AccountChanged(val signedIn: Boolean) : Msg

        data class ThemeChanged(val theme: ThemeMode) : Msg
    }
}
