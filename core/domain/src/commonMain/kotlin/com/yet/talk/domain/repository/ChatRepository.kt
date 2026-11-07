package com.yet.talk.domain.repository

import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.domain.model.chat.Disappearing
import com.yet.talk.domain.model.media.ImageFile
import kotlinx.coroutines.flow.Flow

/**
 * Private chats (NIP-17 over NIP-44 and NIP-59): end-to-end encrypted with the viewer's key, delivered through each
 * member's inbox relays (kind 10050), stored encrypted on the device. Incoming chats sync while the app runs.
 */
interface ChatRepository {
    /** Publishes the viewer's inbox relays (kind 10050) if they have none: called when Chats opens. */
    suspend fun prepare(): Result<Unit>

    /** Every chat, newest first, requests included (see [Chat.isRequest]); left groups and blocked people left out. */
    fun chats(): Flow<List<Chat>>

    /** Unread messages in chats that are neither requests nor muted, for the navigation badge. */
    fun unreadCount(): Flow<Int>

    /** The chat, also before its first message (a new chat or group). */
    fun chat(id: ChatId): Flow<Chat?>

    fun messages(id: ChatId): Flow<List<ChatMessage>>

    /** False when a member has no inbox relays yet ("Can't receive chats yet"). */
    suspend fun canReceive(id: ChatId): Result<Boolean>

    /** Starts a group (up to [MAX_GROUP_MEMBERS] others) with an optional [name]; the name goes with its messages. */
    suspend fun createGroup(id: ChatId, name: String?): Result<Unit>

    /** Sends a message, optionally replying to [replyTo]: shown at once as Sending…, then sent or Not sent. */
    suspend fun send(id: ChatId, text: String, replyTo: String? = null): Result<Unit>

    /** Encrypts [file] (AES-GCM), uploads it to Blossom and sends it as a file message (kind 15). */
    suspend fun sendImage(id: ChatId, file: ImageFile): Result<Unit>

    /** The decrypted bytes of an image message. */
    suspend fun image(messageId: String): Result<ByteArray>

    /** Sends a Not sent message again. */
    suspend fun retry(messageId: String): Result<Unit>

    /** Reacts with [emoji]; a newer reaction replaces your previous one on that message. */
    suspend fun react(messageId: String, emoji: String): Result<Unit>

    /** Asks every member's app to remove one of your messages; other apps may still show it. */
    suspend fun deleteForEveryone(messageId: String): Result<Unit>

    /** Removes a message from this device only. */
    suspend fun deleteForMe(messageId: String)

    suspend fun setDisappearing(id: ChatId, disappearing: Disappearing)

    suspend fun setMuted(id: ChatId, muted: Boolean)

    /** Leaves a group on this device: it is hidden; its members can still message it. */
    suspend fun leave(id: ChatId)

    suspend fun markRead(id: ChatId)

    /** Accept a request: it moves to Chats. */
    suspend fun accept(id: ChatId)

    /** Removes the chat from this device. */
    suspend fun delete(id: ChatId)

    companion object {
        /** NIP-17 wraps every message once per member, so groups stay small. */
        const val MAX_GROUP_MEMBERS = 10
        val REACTIONS = listOf("👍", "❤️", "😂", "😮", "🙏")
    }
}
