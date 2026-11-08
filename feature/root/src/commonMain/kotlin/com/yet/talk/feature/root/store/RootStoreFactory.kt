package com.yet.talk.feature.root.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.PreferencesRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Inject
internal class RootStoreFactory(
    private val storeFactory: StoreFactory,
    private val accounts: AccountRepository,
    private val preferences: PreferencesRepository,
) {
    fun create(): RootStore =
        object : RootStore, Store<Nothing, RootStore.State, Nothing> by storeFactory.create(
            name = "RootStore",
            initialState = RootStore.State(),
            bootstrapper = SimpleBootstrapper(RootStore.Action.ObserveAccount),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<Nothing, RootStore.Action, RootStore.State, RootStore.Msg, Nothing>() {
        override fun executeAction(action: RootStore.Action) {
            scope.launch {
                combine(accounts.account, accounts.lock) { account, lock ->
                    when {
                        lock.locked -> RootStore.Access.Locked
                        account != null -> RootStore.Access.SignedIn
                        else -> RootStore.Access.SignedOut
                    }
                }.collect { dispatch(RootStore.Msg.AccessChanged(it)) }
            }
            scope.launch { preferences.theme.collect { dispatch(RootStore.Msg.ThemeChanged(it)) } }
            scope.launch { preferences.privacy.collect { dispatch(RootStore.Msg.PrivacyChanged(it)) } }
        }
    }

    private object ReducerImpl : Reducer<RootStore.State, RootStore.Msg> {
        override fun RootStore.State.reduce(msg: RootStore.Msg): RootStore.State =
            when (msg) {
                is RootStore.Msg.AccessChanged -> copy(access = msg.access)
                is RootStore.Msg.ThemeChanged -> copy(theme = msg.theme)
                is RootStore.Msg.PrivacyChanged -> copy(privacy = msg.privacy)
            }
    }
}
