package com.yet.talk.database.dao

import com.yet.talk.database.model.ChatMessageRecord
import com.yet.talk.database.model.ChatRecord
import kotlinx.coroutines.flow.Flow

interface ChatDao {
    /** Stores a message and creates or bumps its chat; [accepted] when the viewer wrote it. False if already stored. */
    suspend fun insert(chatId: String, members: List<String>, message: ChatMessageRecord, accepted: Boolean): Boolean

    suspend fun setState(messageId: String, state: String)

    suspend fun message(id: String): ChatMessageRecord?

    fun observeChats(viewer: String): Flow<List<ChatRecord>>

    fun observeMessages(chatId: String): Flow<List<ChatMessageRecord>>

    suspend fun markRead(chatId: String, at: Long)

    suspend fun accept(chatId: String)

    suspend fun delete(chatId: String)
}
