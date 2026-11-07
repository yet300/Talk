package com.yet.talk.feature.chat.component.info.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.ChatRepository
import com.yet.talk.domain.repository.ModerationRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.feature.chat.component.info.store.ChatInfoStore.Intent
import com.yet.talk.feature.chat.component.info.store.ChatInfoStore.Label
import com.yet.talk.feature.chat.component.info.store.ChatInfoStore.Msg
import com.yet.talk.feature.chat.component.info.store.ChatInfoStore.State
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Inject
internal class ChatInfoStoreFactory(
    private val storeFactory: StoreFactory,
    private val chats: ChatRepository,
    private val profiles: ProfileRepository,
    private val moderation: ModerationRepository,
) {
    fun create(id: ChatId): ChatInfoStore =
        object : ChatInfoStore, Store<Intent, State, Label> by storeFactory.create(
            name = "ChatInfoStore",
            initialState = State(id),
            bootstrapper = SimpleBootstrapper(ChatInfoStore.Action.Load),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<Intent, ChatInfoStore.Action, State, Msg, Label>() {
        override fun executeAction(action: ChatInfoStore.Action) {
            val id = state().id
            scope.launch { chats.chat(id).collect { dispatch(Msg.ChatChanged(it)) } }
            scope.launch { combine(id.members.map(profiles::observeUser)) { it.toList() }.collect { dispatch(Msg.MembersChanged(it)) } }
            scope.launch { moderation.moderation.map { it.blocked }.collect { dispatch(Msg.BlockedChanged(it)) } }
        }

        override fun executeIntent(intent: Intent) {
            val id = state().id
            when (intent) {
                is Intent.SetDisappearing -> scope.launch { chats.setDisappearing(id, intent.disappearing) }
                is Intent.SetMuted -> scope.launch { chats.setMuted(id, intent.muted) }
                is Intent.SetBlocked -> scope.launch {
                    val person = id.members.single()
                    val result = if (intent.blocked) moderation.block(person) else moderation.unblock(person)
                    result.onSuccess { if (intent.blocked) publish(Label.Closed) }.onFailure { dispatch(Msg.Failed(it as? DomainError)) }
                }
                Intent.Leave -> scope.launch {
                    chats.leave(id)
                    publish(Label.Closed)
                }
                Intent.DismissError -> dispatch(Msg.Failed(null))
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.ChatChanged -> copy(chat = msg.chat)
            is Msg.MembersChanged -> copy(members = msg.members)
            is Msg.BlockedChanged -> copy(blocked = msg.blocked)
            is Msg.Failed -> copy(error = msg.error)
        }
    }
}
