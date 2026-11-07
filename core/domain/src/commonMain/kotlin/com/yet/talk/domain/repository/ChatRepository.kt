package com.yet.talk.domain.repository

import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Private chats (NIP-17 over NIP-44 and NIP-59): end-to-end encrypted with the viewer's key, delivered through each
 * member's inbox relays (kind 10050), stored encrypted on the device. Incoming chats sync while the app runs.
 */
interface ChatRepository {
    /** Publishes the viewer's inbox relays (kind 10050) if they have none: called when Chats opens. */
    suspend fun prepare(): Result<Unit>

    /** Every chat, newest first, requests included (see [Chat.isRequest]). */
    fun chats(): Flow<List<Chat>>

    /** Unread messages in chats that are not requests, for the navigation badge. */
    fun unreadCount(): Flow<Int>

    fun messages(id: ChatId): Flow<List<ChatMessage>>

    /** False when a member has no inbox relays yet ("Can't receive chats yet"). */
    suspend fun canReceive(id: ChatId): Result<Boolean>

    /** Sends a message: shown at once as Sending…, then sent or Not sent. */
    suspend fun send(id: ChatId, text: String): Result<Unit>

    /** Sends a Not sent message again. */
    suspend fun retry(messageId: String): Result<Unit>

    suspend fun markRead(id: ChatId)

    /** Accept a request: it moves to Chats. */
    suspend fun accept(id: ChatId)

    /** Removes the chat from this device. */
    suspend fun delete(id: ChatId)
}
