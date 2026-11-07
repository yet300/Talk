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
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.rule.ContentRules
import com.yet.talk.feature.chat.component.conversation.store.ConversationStore
import com.yet.talk.feature.chat.component.conversation.store.ConversationStoreFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

internal class DefaultConversationComponent(
    componentContext: ComponentContext,
    chatId: ChatId,
    private val output: (ConversationComponent.Output) -> Unit,
    storeFactory: ConversationStoreFactory,
) : ConversationComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore(key = "conversation-${chatId.value}") { storeFactory.create(chatId) }

    init {
        coroutineScope().launch {
            store.labels.collect { if (it == ConversationStore.Label.Deleted) output(ConversationComponent.Output.Deleted) }
        }
    }

    override val model: Value<ConversationComponent.Model> = store.asValue().map { state ->
        val isRequest = state.chat?.isRequest == true && !state.accepted
        ConversationComponent.Model(
            id = state.id,
            members = state.members,
            messages = state.messages,
            text = state.text,
            canSend = state.text.isNotBlank() && state.text.length <= ContentRules.MAX_REPLY_LENGTH && state.canReceive != false && !isRequest,
            canReceive = state.canReceive,
            isRequest = isRequest,
            error = state.error,
        )
    }

    override fun onTextChanged(text: String) = store.accept(ConversationStore.Intent.ChangeText(text))

    override fun onSend() = store.accept(ConversationStore.Intent.Send)

    override fun onRetry(message: ChatMessage) = store.accept(ConversationStore.Intent.Retry(message))

    override fun onAccept() = store.accept(ConversationStore.Intent.Accept)

    override fun onDelete() = store.accept(ConversationStore.Intent.Delete)

    override fun onOpenMember(id: UserId) = output(ConversationComponent.Output.OpenProfile(id))

    override fun onBack() = output(ConversationComponent.Output.Back)

    override fun onErrorShown() = store.accept(ConversationStore.Intent.DismissError)
}

@Inject
internal class DefaultConversationComponentFactory(private val storeFactory: ConversationStoreFactory) : ConversationComponent.Factory {
    override fun invoke(componentContext: ComponentContext, chatId: ChatId, output: (ConversationComponent.Output) -> Unit): ConversationComponent =
        DefaultConversationComponent(componentContext, chatId, output, storeFactory)
}
