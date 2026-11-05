package com.yet.talk.feature.auth.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.AccountRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

@Inject
internal class AuthStoreFactory(
    private val storeFactory: StoreFactory,
    private val accounts: AccountRepository,
) {
    fun create(): AuthStore =
        object : AuthStore, Store<AuthStore.Intent, AuthStore.State, Nothing> by storeFactory.create(
            name = "AuthStore",
            initialState = AuthStore.State(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl :
        CoroutineExecutor<AuthStore.Intent, Nothing, AuthStore.State, AuthStore.Msg, Nothing>() {

        override fun executeIntent(intent: AuthStore.Intent) {
            when (intent) {
                is AuthStore.Intent.ChangeKey -> dispatch(AuthStore.Msg.KeyChanged(intent.key))
                AuthStore.Intent.ImportKey -> signIn { accounts.importKey(state().key) }
                AuthStore.Intent.CreateKey -> signIn { accounts.createAccount() }
                AuthStore.Intent.DismissError -> dispatch(AuthStore.Msg.ErrorDismissed)
            }
        }

        private fun signIn(block: suspend () -> Result<Account>) {
            if (state().isWorking) return
            dispatch(AuthStore.Msg.Started)
            scope.launch {
                dispatch(AuthStore.Msg.Finished(block().exceptionOrNull() as? DomainError))
            }
        }
    }

    private object ReducerImpl : Reducer<AuthStore.State, AuthStore.Msg> {
        override fun AuthStore.State.reduce(msg: AuthStore.Msg): AuthStore.State =
            when (msg) {
                is AuthStore.Msg.KeyChanged -> copy(key = msg.key, error = null)
                AuthStore.Msg.Started -> copy(isWorking = true, error = null)
                // The key never stays in memory longer than needed: cleared once used.
                is AuthStore.Msg.Finished -> copy(isWorking = false, error = msg.error, key = if (msg.error == null) "" else key)
                AuthStore.Msg.ErrorDismissed -> copy(error = null)
            }
    }
}
