package com.yet.talk.feature.auth.lock.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.feature.auth.lock.store.LockStore.Intent
import com.yet.talk.feature.auth.lock.store.LockStore.Msg
import com.yet.talk.feature.auth.lock.store.LockStore.State
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

@Inject
internal class LockStoreFactory(private val storeFactory: StoreFactory, private val accounts: AccountRepository) {

    fun create(): LockStore =
        object : LockStore, Store<Intent, State, Nothing> by storeFactory.create(
            name = "LockStore",
            initialState = State(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<Intent, Nothing, State, Msg, Nothing>() {
        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.ChangePasscode -> dispatch(Msg.PasscodeChanged(intent.passcode))
                // The root leaves the lock screen once the account is open.
                Intent.Unlock -> if (!state().isUnlocking && state().passcode.isNotEmpty()) {
                    dispatch(Msg.Unlocking(true))
                    scope.launch {
                        val result = accounts.unlock(state().passcode)
                        dispatch(Msg.Unlocking(false))
                        result.onFailure {
                            dispatch(Msg.PasscodeChanged(""))
                            dispatch(Msg.Failed(it as? DomainError))
                        }
                    }
                }
                Intent.Forgot -> scope.launch { accounts.signOut() }
                Intent.DismissError -> dispatch(Msg.Failed(null))
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.PasscodeChanged -> copy(passcode = msg.passcode)
            is Msg.Unlocking -> copy(isUnlocking = msg.unlocking)
            is Msg.Failed -> copy(error = msg.error)
        }
    }
}
