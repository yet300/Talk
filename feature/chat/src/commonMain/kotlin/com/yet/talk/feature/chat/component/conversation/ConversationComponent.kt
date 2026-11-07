package com.yet.talk.feature.chat.component.conversation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId

/** One private chat (`Conversation`): the messages, the composer, and Accept / Delete for a request. */
interface ConversationComponent {
    val model: Value<Model>

    fun onTextChanged(text: String)

    fun onSend()

    /** Retry on a Not sent message. */
    fun onRetry(message: ChatMessage)

    /** Accept a request: it moves to Chats and the composer appears. */
    fun onAccept()

    /** Delete a request (or the chat) from this device. */
    fun onDelete()

    fun onOpenMember(id: UserId)

    fun onBack()

    fun onErrorShown()

    data class Model(
        val id: ChatId,
        /** The other members; placeholders until their profiles are known. */
        val members: List<User>,
        val messages: List<ChatMessage>,
        val text: String,
        val canSend: Boolean,
        /** False when a member has no inbox relays: the composer is off and a banner says why. Null while checking. */
        val canReceive: Boolean?,
        val isRequest: Boolean,
        val error: DomainError?,
    )

    sealed interface Output {
        data object Back : Output

        data object Deleted : Output

        data class OpenProfile(val id: UserId) : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, chatId: ChatId, output: (Output) -> Unit): ConversationComponent
    }
}
