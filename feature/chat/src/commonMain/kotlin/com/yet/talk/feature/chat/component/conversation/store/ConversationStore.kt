package com.yet.talk.feature.chat.component.conversation.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.model.user.User
import com.yet.talk.feature.chat.component.conversation.ConversationComponent

internal interface ConversationStore : Store<ConversationStore.Intent, ConversationStore.State, ConversationStore.Label> {

    data class State(
        val id: ChatId,
        val chat: Chat? = null,
        val members: List<User> = emptyList(),
        val messages: List<ChatMessage> = emptyList(),
        val text: String = "",
        val replyingTo: ChatMessage? = null,
        val isUploading: Boolean = false,
        /** Images by message id, once asked for. */
        val images: Map<String, ConversationComponent.ImageState> = emptyMap(),
        val canReceive: Boolean? = null,
        val accepted: Boolean = false,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class ChangeText(val text: String) : Intent

        data object Send : Intent

        data class Retry(val message: ChatMessage) : Intent

        data class Reply(val message: ChatMessage?) : Intent

        data class React(val message: ChatMessage, val emoji: String) : Intent

        data class DeleteForEveryone(val message: ChatMessage) : Intent

        data class DeleteForMe(val message: ChatMessage) : Intent

        data class SendImage(val file: ImageFile) : Intent

        data class LoadImage(val message: ChatMessage) : Intent

        data object Accept : Intent

        data object Delete : Intent

        data object Block : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object Load : Action
    }

    sealed interface Msg {
        data class ChatChanged(val chat: Chat?) : Msg

        data class MembersChanged(val members: List<User>) : Msg

        data class MessagesChanged(val messages: List<ChatMessage>) : Msg

        data class TextChanged(val text: String) : Msg

        data class ReplyingTo(val message: ChatMessage?) : Msg

        data class Uploading(val uploading: Boolean) : Msg

        data class ImageChanged(val id: String, val image: ConversationComponent.ImageState) : Msg

        data class CanReceive(val canReceive: Boolean) : Msg

        data object Accepted : Msg

        data class Failed(val error: DomainError?) : Msg
    }

    sealed interface Label {
        /** The chat is gone from the list (deleted, or its person blocked). */
        data object Closed : Label
    }
}
