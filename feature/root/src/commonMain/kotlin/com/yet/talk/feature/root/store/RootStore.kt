package com.yet.talk.feature.root.store

import com.arkivanov.mvikotlin.core.store.Store

/** Whether someone is signed in; null until the saved key has been read. */
internal interface RootStore : Store<Nothing, RootStore.State, Nothing> {

    data class State(val signedIn: Boolean? = null)

    sealed interface Action {
        data object ObserveAccount : Action
    }

    sealed interface Msg {
        data class AccountChanged(val signedIn: Boolean) : Msg
    }
}
