package com.yet.talk.feature.chat.component.conversation.store

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
import com.yet.talk.feature.chat.component.conversation.store.ConversationStore.Intent
import com.yet.talk.feature.chat.component.conversation.store.ConversationStore.Label
import com.yet.talk.feature.chat.component.conversation.store.ConversationStore.Msg
import com.yet.talk.feature.chat.component.conversation.store.ConversationStore.State
import com.yet.talk.feature.chat.component.conversation.ConversationComponent.ImageState
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Inject
internal class ConversationStoreFactory(
    private val storeFactory: StoreFactory,
    private val chats: ChatRepository,
    private val profiles: ProfileRepository,
    private val moderation: ModerationRepository,
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
            scope.launch { chats.chat(id).collect { dispatch(Msg.ChatChanged(it)) } }
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
                    val replyTo = state.replyingTo?.id
                    dispatch(Msg.TextChanged(""))
                    dispatch(Msg.ReplyingTo(null))
                    // The message shows at once (Sending…); a failure leaves it as Not sent with Retry.
                    run { chats.send(state.id, text, replyTo) }
                }
                is Intent.Retry -> run { chats.retry(intent.message.id) }
                is Intent.Reply -> dispatch(Msg.ReplyingTo(intent.message))
                is Intent.React -> run { chats.react(intent.message.id, intent.emoji) }
                is Intent.DeleteForEveryone -> run { chats.deleteForEveryone(intent.message.id) }
                is Intent.DeleteForMe -> scope.launch { chats.deleteForMe(intent.message.id) }
                is Intent.SendImage -> scope.launch {
                    dispatch(Msg.Uploading(true))
                    val result = chats.sendImage(state.id, intent.file)
                    dispatch(Msg.Uploading(false))
                    result.onFailure { dispatch(Msg.Failed(it as? DomainError)) }
                }
                is Intent.LoadImage -> if (intent.message.id !in state.images) {
                    dispatch(Msg.ImageChanged(intent.message.id, ImageState.Loading))
                    scope.launch {
                        val image = chats.image(intent.message.id).fold({ ImageState.Loaded(it) }, { ImageState.Failed })
                        dispatch(Msg.ImageChanged(intent.message.id, image))
                    }
                }
                Intent.Accept -> scope.launch {
                    chats.accept(state.id)
                    dispatch(Msg.Accepted)
                }
                Intent.Delete -> scope.launch {
                    chats.delete(state.id)
                    publish(Label.Closed)
                }
                Intent.Block -> scope.launch {
                    val failure = state.id.members.firstNotNullOfOrNull { moderation.block(it).exceptionOrNull() }
                    if (failure == null) publish(Label.Closed) else dispatch(Msg.Failed(failure as? DomainError))
                }
                Intent.DismissError -> dispatch(Msg.Failed(null))
            }
        }

        private fun run(action: suspend () -> Result<Unit>) {
            scope.launch { action().onFailure { dispatch(Msg.Failed(it as? DomainError)) } }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.ChatChanged -> copy(chat = msg.chat)
            is Msg.MembersChanged -> copy(members = msg.members)
            is Msg.MessagesChanged -> copy(messages = msg.messages)
            is Msg.TextChanged -> copy(text = msg.text)
            is Msg.ReplyingTo -> copy(replyingTo = msg.message)
            is Msg.Uploading -> copy(isUploading = msg.uploading)
            is Msg.ImageChanged -> copy(images = images + (msg.id to msg.image))
            is Msg.CanReceive -> copy(canReceive = msg.canReceive)
            Msg.Accepted -> copy(accepted = true)
            is Msg.Failed -> copy(error = msg.error)
        }
    }
}
