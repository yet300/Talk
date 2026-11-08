package com.yet.talk.feature.tab.chats.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.ChatRepository
import com.yet.talk.feature.tab.chats.store.ChatsStore.Action
import com.yet.talk.feature.tab.chats.store.ChatsStore.Intent
import com.yet.talk.feature.tab.chats.store.ChatsStore.Msg
import com.yet.talk.feature.tab.chats.store.ChatsStore.State
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

@Inject
internal class ChatsStoreFactory(private val storeFactory: StoreFactory, private val chats: ChatRepository) {

    fun create(): ChatsStore =
        object : ChatsStore, Store<Intent, State, Nothing> by storeFactory.create(
            name = "ChatsStore",
            initialState = State(),
            bootstrapper = SimpleBootstrapper(Action.Load),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<Intent, Action, State, Msg, Nothing>() {
        override fun executeAction(action: Action) {
            scope.launch { chats.chats().collect { dispatch(Msg.ChatsChanged(it)) } }
            // Others can write to you only once your inbox relays are known.
            scope.launch { chats.prepare().onFailure { dispatch(Msg.Failed(it as? DomainError)) } }
        }

        override fun executeIntent(intent: Intent) = when (intent) {
            Intent.DismissError -> dispatch(Msg.Failed(null))
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.ChatsChanged -> copy(chats = msg.chats, isLoading = false)
            is Msg.Failed -> copy(error = msg.error)
        }
    }
}
