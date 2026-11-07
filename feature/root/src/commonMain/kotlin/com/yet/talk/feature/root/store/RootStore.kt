package com.yet.talk.feature.root.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.settings.ThemeMode

/** Whether someone is signed in, signed out or locked (null until the saved key has been read), and the app theme. */
internal interface RootStore : Store<Nothing, RootStore.State, Nothing> {

    data class State(val access: Access? = null, val theme: ThemeMode = ThemeMode.System)

    enum class Access { SignedOut, Locked, SignedIn }

    sealed interface Action {
        data object ObserveAccount : Action
    }

    sealed interface Msg {
        data class AccessChanged(val access: Access) : Msg

        data class ThemeChanged(val theme: ThemeMode) : Msg
    }
}
