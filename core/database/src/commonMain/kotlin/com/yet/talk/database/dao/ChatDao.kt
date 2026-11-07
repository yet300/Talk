package com.yet.talk.database.dao

import com.yet.talk.database.model.ChatMessageRecord
import com.yet.talk.database.model.ChatReactionRecord
import com.yet.talk.database.model.ChatRecord
import kotlinx.coroutines.flow.Flow

interface ChatDao {
    /** Stores a message and creates or bumps its chat; [accepted] when the viewer wrote it. False if already stored. */
    suspend fun insert(chatId: String, members: List<String>, message: ChatMessageRecord, accepted: Boolean): Boolean

    /** Creates the chat before its first message (a new group, or a setting changed); the viewer started it. */
    suspend fun ensureChat(chatId: String, members: List<String>, at: Long)

    suspend fun setState(messageId: String, state: String)

    suspend fun message(id: String): ChatMessageRecord?

    fun observeChats(viewer: String): Flow<List<ChatRecord>>

    fun observeMessages(chatId: String): Flow<List<ChatMessageRecord>>

    fun observeReactions(chatId: String): Flow<List<ChatReactionRecord>>

    /** Keeps the newest reaction of [author] on [messageId]. */
    suspend fun react(messageId: String, author: String, emoji: String, at: Long)

    /** Wipes a message's content and hides it ([com.yet.talk.database.model.MessageRemoval]). */
    suspend fun remove(messageId: String, removal: Long)

    /** Turns messages whose time is up into "expired" placeholders. */
    suspend fun expire(now: Long)

    suspend fun markRead(chatId: String, at: Long)

    suspend fun accept(chatId: String)

    suspend fun delete(chatId: String)

    suspend fun setName(chatId: String, nameEnc: String?)

    suspend fun setMuted(chatId: String, muted: Boolean)

    suspend fun setHidden(chatId: String, hidden: Boolean)

    suspend fun setExpireSeconds(chatId: String, seconds: Long?)

    /** The chat's encrypted name and disappearing setting, when it exists. */
    suspend fun settings(chatId: String): Pair<String?, Long?>?

    /** Verified contacts of [owner] and when they were verified. */
    fun observeVerified(owner: String): Flow<Map<String, Long>>

    suspend fun setVerified(owner: String, pubkey: String, at: Long?)

    /** Removes every chat, message, reaction and verification from this device. */
    suspend fun eraseChats()

    fun observeBlocked(owner: String): Flow<List<String>>

    suspend fun setBlocked(owner: String, pubkeys: Collection<String>)

    /** Keeps the newest mute list event (JSON) of [owner]. */
    suspend fun storeMuteList(owner: String, createdAt: Long, eventJson: String)

    suspend fun muteList(owner: String): String?
}
