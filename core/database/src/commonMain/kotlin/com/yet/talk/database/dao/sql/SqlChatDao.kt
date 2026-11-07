package com.yet.talk.database.dao.sql

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.ChatDao
import com.yet.talk.database.model.ChatMessageRecord
import com.yet.talk.database.model.ChatReactionRecord
import com.yet.talk.database.model.ChatRecord
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
                with(message) { queries.insertMessage(id, chatId, author, createdAt, rumorEnc, publishState, kind, replyTo, expiresAt) } > 0
            }
        }

    override suspend fun ensureChat(chatId: String, members: List<String>, at: Long) =
        io { chatQueries.insertChatIfAbsent(chatId, members.joinToString(","), 1, at) }

    override suspend fun setState(messageId: String, state: String) = io { chatQueries.setMessageState(state, messageId) }

    override suspend fun message(id: String): ChatMessageRecord? = withContext(dispatchers.io) {
        holder.get().chatQueries.selectMessage(id, ::ChatMessageRecord).awaitAsOneOrNull()
    }

    override fun observeChats(viewer: String): Flow<List<ChatRecord>> =
        holder.observeList(dispatchers.io) {
            chatQueries.selectChats(viewer) { id, members, lastAt, nameEnc, muted, expire, isRequest, unread, lastRumor, lastState ->
                ChatRecord(
                    id, members.split(",").filter(String::isNotEmpty), lastAt, nameEnc, muted != 0L, expire,
                    isRequest == true, unread, lastRumor, lastState,
                )
            }
        }

    override fun observeMessages(chatId: String): Flow<List<ChatMessageRecord>> =
        holder.observeList(dispatchers.io) { chatQueries.selectMessages(chatId, ::ChatMessageRecord) }

    override fun observeReactions(chatId: String): Flow<List<ChatReactionRecord>> =
        holder.observeList(dispatchers.io) { chatQueries.selectReactions(chatId, ::ChatReactionRecord) }

    override suspend fun react(messageId: String, author: String, emoji: String, at: Long) = withContext(dispatchers.io) {
        val database = holder.get()
        database.transaction {
            database.chatQueries.deleteOlderReaction(messageId, author, at)
            database.chatQueries.insertReaction(messageId, author, emoji, at)
        }
    }

    override suspend fun remove(messageId: String, removal: Long) = io { chatQueries.removeMessage(removal, messageId) }

    override suspend fun expire(now: Long) = io { chatQueries.expireMessages(now) }

    override suspend fun markRead(chatId: String, at: Long) = io { chatQueries.markRead(at, chatId) }

    override suspend fun accept(chatId: String) = io { chatQueries.accept(chatId) }

    override suspend fun delete(chatId: String) = io { chatQueries.deleteChat(chatId) }

    override suspend fun setName(chatId: String, nameEnc: String?) = io { chatQueries.setName(nameEnc, chatId) }

    override suspend fun setMuted(chatId: String, muted: Boolean) = io { chatQueries.setMuted(if (muted) 1 else 0, chatId) }

    override suspend fun setHidden(chatId: String, hidden: Boolean) = io { chatQueries.setHidden(if (hidden) 1 else 0, chatId) }

    override suspend fun setExpireSeconds(chatId: String, seconds: Long?) = io { chatQueries.setExpire(seconds, chatId) }

    override suspend fun settings(chatId: String): Pair<String?, Long?>? = withContext(dispatchers.io) {
        holder.get().chatQueries.selectChat(chatId) { name, expire -> name to expire }.awaitAsOneOrNull()
    }

    override fun observeVerified(owner: String): Flow<Map<String, Long>> =
        holder.observeList(dispatchers.io) { chatQueries.selectVerified(owner) { pubkey, at -> pubkey to at } }.map { it.toMap() }

    override suspend fun setVerified(owner: String, pubkey: String, at: Long?) =
        if (at == null) io { chatQueries.unsetVerified(owner, pubkey) } else io { chatQueries.setVerified(owner, pubkey, at) }

    override suspend fun eraseChats() = io { chatQueries.eraseChats() }

    override fun observeBlocked(owner: String): Flow<List<String>> =
        holder.observeList(dispatchers.io) { chatQueries.selectBlocked(owner) }

    override suspend fun setBlocked(owner: String, pubkeys: Collection<String>) = withContext(dispatchers.io) {
        val database = holder.get()
        database.transaction {
            database.chatQueries.replaceBlocked(owner).await()
            pubkeys.forEach { database.chatQueries.insertBlocked(owner, it) }
        }
    }

    override suspend fun storeMuteList(owner: String, createdAt: Long, eventJson: String) = io { chatQueries.upsertMuteList(owner, createdAt, eventJson) }

    override suspend fun muteList(owner: String): String? = withContext(dispatchers.io) {
        holder.get().chatQueries.selectMuteList(owner).awaitAsOneOrNull()
    }

    /** Runs a write and awaits it: grouped statements return a lazy [QueryResult.AsyncValue] that does nothing until then. */
    private suspend fun io(block: suspend com.yet.talk.database.TalkDatabase.() -> Any?) = withContext(dispatchers.io) {
        (holder.get().block() as? QueryResult<*>)?.await()
        Unit
    }
}
