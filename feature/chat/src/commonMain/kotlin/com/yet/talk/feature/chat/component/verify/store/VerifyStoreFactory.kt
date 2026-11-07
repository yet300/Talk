package com.yet.talk.feature.chat.component.verify.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.ChatRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.feature.chat.component.verify.store.VerifyStore.Intent
import com.yet.talk.feature.chat.component.verify.store.VerifyStore.Msg
import com.yet.talk.feature.chat.component.verify.store.VerifyStore.State
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Inject
internal class VerifyStoreFactory(
    private val storeFactory: StoreFactory,
    private val chats: ChatRepository,
    private val profiles: ProfileRepository,
) {
    fun create(person: UserId): VerifyStore =
        object : VerifyStore, Store<Intent, State, Nothing> by storeFactory.create(
            name = "VerifyStore",
            initialState = State(person),
            bootstrapper = SimpleBootstrapper(VerifyStore.Action.Load),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<Intent, VerifyStore.Action, State, Msg, Nothing>() {
        override fun executeAction(action: VerifyStore.Action) {
            val person = state().person
            scope.launch { profiles.observeUser(person).collect { dispatch(Msg.UserChanged(it)) } }
            scope.launch { chats.verified().map { it[person] }.collect { dispatch(Msg.VerifiedChanged(it)) } }
            scope.launch {
                chats.safetyCode(person).onSuccess { dispatch(Msg.CodeLoaded(it)) }.onFailure { dispatch(Msg.Failed(it as? DomainError)) }
            }
        }

        override fun executeIntent(intent: Intent) {
            val person = state().person
            when (intent) {
                is Intent.SetVerified -> scope.launch { chats.setVerified(person, intent.verified) }
                is Intent.Scanned -> scope.launch {
                    chats.verifyScanned(person, intent.payload)
                        .onSuccess { dispatch(Msg.ScanMismatch(!it)) }
                        .onFailure { dispatch(Msg.Failed(it as? DomainError)) }
                }
                Intent.DismissError -> {
                    dispatch(Msg.Failed(null))
                    dispatch(Msg.ScanMismatch(false))
                }
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.UserChanged -> copy(user = msg.user)
            is Msg.CodeLoaded -> copy(code = msg.code)
            is Msg.VerifiedChanged -> copy(verifiedAt = msg.at)
            is Msg.ScanMismatch -> copy(scanMismatch = msg.mismatch)
            is Msg.Failed -> copy(error = msg.error)
        }
    }
}
