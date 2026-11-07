package com.yet.talk.feature.chat.component.conversation

import com.app.common.decompose.asValue
import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.ChatRepository
import com.yet.talk.domain.rule.ContentRules
import com.yet.talk.feature.chat.component.conversation.store.ConversationStore
import com.yet.talk.feature.chat.component.conversation.store.ConversationStore.Intent
import com.yet.talk.feature.chat.component.conversation.store.ConversationStoreFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

internal class DefaultConversationComponent(
    componentContext: ComponentContext,
    private val chatId: ChatId,
    private val output: (ConversationComponent.Output) -> Unit,
    storeFactory: ConversationStoreFactory,
) : ConversationComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore(key = "conversation-${chatId.value}") { storeFactory.create(chatId) }

    init {
        coroutineScope().launch {
            store.labels.collect { if (it == ConversationStore.Label.Closed) output(ConversationComponent.Output.Closed) }
        }
    }

    override val model: Value<ConversationComponent.Model> = store.asValue().map { state ->
        val isRequest = state.chat?.isRequest == true && !state.accepted
        ConversationComponent.Model(
            id = state.id,
            members = state.members,
            name = state.chat?.name,
            isGroup = state.id.isGroup,
            messages = state.messages,
            text = state.text,
            replyingTo = state.replyingTo,
            isUploading = state.isUploading,
            images = state.images,
            canSend = state.text.isNotBlank() && state.text.length <= ContentRules.MAX_REPLY_LENGTH && state.canReceive != false && !isRequest,
            canReceive = state.canReceive,
            isRequest = isRequest,
            disappearing = state.chat?.disappearing,
            reactions = ChatRepository.REACTIONS,
            error = state.error,
        )
    }

    override fun onTextChanged(text: String) = store.accept(Intent.ChangeText(text))

    override fun onSend() = store.accept(Intent.Send)

    override fun onRetry(message: ChatMessage) = store.accept(Intent.Retry(message))

    override fun onReply(message: ChatMessage?) = store.accept(Intent.Reply(message))

    override fun onReact(message: ChatMessage, emoji: String) = store.accept(Intent.React(message, emoji))

    override fun onDeleteForEveryone(message: ChatMessage) = store.accept(Intent.DeleteForEveryone(message))

    override fun onDeleteForMe(message: ChatMessage) = store.accept(Intent.DeleteForMe(message))

    override fun onSendImage(file: ImageFile) = store.accept(Intent.SendImage(file))

    override fun onShowImage(message: ChatMessage) = store.accept(Intent.LoadImage(message))

    override fun onAccept() = store.accept(Intent.Accept)

    override fun onDelete() = store.accept(Intent.Delete)

    override fun onBlock() = store.accept(Intent.Block)

    override fun onOpenInfo() = output(ConversationComponent.Output.OpenInfo(chatId))

    override fun onOpenMember(id: UserId) = output(ConversationComponent.Output.OpenProfile(id))

    override fun onBack() = output(ConversationComponent.Output.Back)

    override fun onErrorShown() = store.accept(Intent.DismissError)
}

@Inject
internal class DefaultConversationComponentFactory(private val storeFactory: ConversationStoreFactory) : ConversationComponent.Factory {
    override fun invoke(componentContext: ComponentContext, chatId: ChatId, output: (ConversationComponent.Output) -> Unit): ConversationComponent =
        DefaultConversationComponent(componentContext, chatId, output, storeFactory)
}
