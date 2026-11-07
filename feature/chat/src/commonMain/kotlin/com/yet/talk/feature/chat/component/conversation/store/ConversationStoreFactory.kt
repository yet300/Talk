package com.yet.talk.feature.chat.component.conversation.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.ChatRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.feature.chat.component.conversation.store.ConversationStore.Intent
import com.yet.talk.feature.chat.component.conversation.store.ConversationStore.Label
import com.yet.talk.feature.chat.component.conversation.store.ConversationStore.Msg
import com.yet.talk.feature.chat.component.conversation.store.ConversationStore.State
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Inject
internal class ConversationStoreFactory(private val storeFactory: StoreFactory, private val chats: ChatRepository,
    private val profiles: ProfileRepository,
) {

    fun create(id: ChatId): ConversationStore =
        object : ConversationStore, Store<Intent, State, Label> by storeFactory.create(
            name = "ConversationStore",
            initialState = State(id),
            bootstrapper = SimpleBootstrapper(ConversationStore.Action.Load),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<Intent, ConversationStore.Action, State, Msg, Label>() {

        override fun executeAction(action: ConversationStore.Action) {
            val id = state().id
            scope.launch { chats.chats().map { list -> list.firstOrNull { it.id == id } }.collect { dispatch(Msg.ChatChanged(it)) } }
            scope.launch {
                combine(id.members.map(profiles::observeUser)) { it.toList() }.collect { dispatch(Msg.MembersChanged(it)) }
            }
            id.members.forEach { scope.launch { profiles.refresh(it) } }
            // Open means read: new messages that arrive while it is open are read too.
            scope.launch {
                chats.messages(id).collect {
                    dispatch(Msg.MessagesChanged(it))
                    chats.markRead(id)
                }
            }
            scope.launch { chats.canReceive(id).onSuccess { dispatch(Msg.CanReceive(it)) } }
        }

        override fun executeIntent(intent: Intent) {
            val state = state()
            when (intent) {
                is Intent.ChangeText -> dispatch(Msg.TextChanged(intent.text))
                Intent.Send -> if (state.text.isNotBlank()) {
                    val text = state.text
                    dispatch(Msg.TextChanged(""))
                    // The message shows at once (Sending…); a failure leaves it as Not sent with Retry.
                    scope.launch { chats.send(state.id, text).onFailure { dispatch(Msg.Failed(it as? DomainError)) } }
                }
                is Intent.Retry -> scope.launch { chats.retry(intent.message.id).onFailure { dispatch(Msg.Failed(it as? DomainError)) } }
                Intent.Accept -> scope.launch {
                    chats.accept(state.id)
                    dispatch(Msg.Accepted)
                }
                Intent.Delete -> scope.launch {
                    chats.delete(state.id)
                    publish(Label.Deleted)
                }
                Intent.DismissError -> dispatch(Msg.Failed(null))
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.ChatChanged -> copy(chat = msg.chat)
            is Msg.MembersChanged -> copy(members = msg.members)
            is Msg.MessagesChanged -> copy(messages = msg.messages)
            is Msg.TextChanged -> copy(text = msg.text)
            is Msg.CanReceive -> copy(canReceive = msg.canReceive)
            Msg.Accepted -> copy(accepted = true)
            is Msg.Failed -> copy(error = msg.error)
        }
    }
}
