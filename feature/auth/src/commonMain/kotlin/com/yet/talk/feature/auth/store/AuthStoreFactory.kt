package com.yet.talk.feature.auth.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.AccountRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

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
                AuthStore.Intent.CreateKey -> createKey()
                AuthStore.Intent.NewKeyCopied -> dispatch(AuthStore.Msg.NewKeyCopied)
                AuthStore.Intent.NewKeySaved -> state().newKey?.takeIf { state().newKeySaveReady }?.let { nsec -> signIn { accounts.importKey(nsec) } }
                AuthStore.Intent.DismissError -> dispatch(AuthStore.Msg.ErrorDismissed)
            }
        }

        private fun createKey() {
            if (state().isWorking) return
            scope.launch {
                accounts.generateKey()
                    .onSuccess { nsec ->
                        dispatch(AuthStore.Msg.KeyCreated(nsec))
                        delay(SAVE_DELAY)
                        dispatch(AuthStore.Msg.SaveReady)
                    }
                    .onFailure { dispatch(AuthStore.Msg.Finished(it as? DomainError)) }
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
                is AuthStore.Msg.Finished ->
                    if (msg.error == null) copy(isWorking = false, error = null, key = "", newKey = null) else copy(isWorking = false, error = msg.error)
                is AuthStore.Msg.KeyCreated -> copy(newKey = msg.nsec, newKeyCopied = false, newKeySaveReady = false, error = null)
                AuthStore.Msg.NewKeyCopied -> copy(newKeyCopied = true, newKeySaveReady = true)
                AuthStore.Msg.SaveReady -> copy(newKeySaveReady = true)
                AuthStore.Msg.ErrorDismissed -> copy(error = null)
            }
    }

    private companion object {
        /** How long "Save your key" waits before "I saved it" unlocks without Copy (the design's 3 seconds). */
        val SAVE_DELAY = 3.seconds
    }
}
