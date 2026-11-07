package com.yet.talk.database.dao.sql

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.ChatDao
import com.yet.talk.database.model.ChatMessageRecord
import com.yet.talk.database.model.ChatRecord
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
internal class SqlChatDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : ChatDao {

    override suspend fun insert(chatId: String, members: List<String>, message: ChatMessageRecord, accepted: Boolean): Boolean =
        withContext(dispatchers.io) {
            val database = holder.get()
            database.transactionWithResult {
                val queries = database.chatQueries
                val flag = if (accepted) 1L else 0L
                queries.insertChatIfAbsent(chatId, members.joinToString(","), flag, message.createdAt)
                queries.touchChat(message.createdAt, flag, chatId)
                with(message) { queries.insertMessage(id, chatId, author, createdAt, rumorEnc, publishState) } > 0
            }
        }

    override suspend fun setState(messageId: String, state: String) = withContext(dispatchers.io) {
        holder.get().chatQueries.setMessageState(state, messageId)
        Unit
    }

    override suspend fun message(id: String): ChatMessageRecord? = withContext(dispatchers.io) {
        holder.get().chatQueries.selectMessage(id, ::ChatMessageRecord).awaitAsOneOrNull()
    }

    override fun observeChats(viewer: String): Flow<List<ChatRecord>> =
        holder.observeList(dispatchers.io) {
            chatQueries.selectChats(viewer) { id, members, lastAt, isRequest, unread, lastRumor, lastState ->
                ChatRecord(id, members.split(",").filter(String::isNotEmpty), lastAt, isRequest == true, unread, lastRumor, lastState)
            }
        }

    override fun observeMessages(chatId: String): Flow<List<ChatMessageRecord>> =
        holder.observeList(dispatchers.io) { chatQueries.selectMessages(chatId, ::ChatMessageRecord) }

    override suspend fun markRead(chatId: String, at: Long) = withContext(dispatchers.io) {
        holder.get().chatQueries.markRead(at, chatId)
        Unit
    }

    override suspend fun accept(chatId: String) = withContext(dispatchers.io) {
        holder.get().chatQueries.accept(chatId)
        Unit
    }

    override suspend fun delete(chatId: String) = withContext(dispatchers.io) {
        holder.get().chatQueries.deleteChat(chatId)
        Unit
    }
}
