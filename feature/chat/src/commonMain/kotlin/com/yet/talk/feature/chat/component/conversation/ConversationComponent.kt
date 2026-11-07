package com.yet.talk.feature.chat.component.conversation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import kotlin.time.Duration

/**
 * One private chat (`Conversation`): the messages with their replies and reactions, the composer (text, image,
 * reply), the message actions, and Accept / Delete / Block for a request. No read receipts, no typing indicator.
 */
interface ConversationComponent {
    val model: Value<Model>

    fun onTextChanged(text: String)

    fun onSend()

    /** Retry on a Not sent message. */
    fun onRetry(message: ChatMessage)

    /** Reply to [message] (shown above the composer); null cancels. */
    fun onReply(message: ChatMessage?)

    fun onReact(message: ChatMessage, emoji: String)

    /** Your own message, for everyone ("Other apps may still show it"). */
    fun onDeleteForEveryone(message: ChatMessage)

    fun onDeleteForMe(message: ChatMessage)

    /** Encrypts and sends an image (PNG, JPEG, GIF or WebP up to 5 MB). */
    fun onSendImage(file: ImageFile)

    /** Downloads and decrypts an image message once it is on screen. */
    fun onShowImage(message: ChatMessage)

    /** Accept a request: it moves to Chats and the composer appears. */
    fun onAccept()

    /** Delete a request (or the chat) from this device. */
    fun onDelete()

    /** Block the person behind a request: it disappears with their other chats. */
    fun onBlock()

    fun onOpenInfo()

    fun onOpenMember(id: UserId)

    fun onBack()

    fun onErrorShown()

    data class Model(
        val id: ChatId,
        /** The other members; placeholders until their profiles are known. */
        val members: List<User>,
        /** A group's name, if it has one. */
        val name: String?,
        val isGroup: Boolean,
        val messages: List<ChatMessage>,
        val text: String,
        val replyingTo: ChatMessage?,
        val isUploading: Boolean,
        val images: Map<String, ImageState>,
        val canSend: Boolean,
        /** False when a member has no inbox relays: the composer is off and a banner says why. Null while checking. */
        val canReceive: Boolean?,
        val isRequest: Boolean,
        val disappearing: Duration?,
        val reactions: List<String>,
        val error: DomainError?,
    )

    sealed interface ImageState {
        data object Loading : ImageState

        class Loaded(val bytes: ByteArray) : ImageState

        data object Failed : ImageState
    }

    sealed interface Output {
        data object Back : Output

        /** Deleted or blocked: the conversation closes. */
        data object Closed : Output

        data class OpenInfo(val id: ChatId) : Output

        data class OpenProfile(val id: UserId) : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, chatId: ChatId, output: (Output) -> Unit): ConversationComponent
    }
}
