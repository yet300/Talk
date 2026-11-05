package com.yet.talk.database.dao.sql

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.ReactionDao
import com.yet.talk.database.model.ReactionRecord
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext

@Inject
internal class SqlReactionDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : ReactionDao {

    override suspend fun insert(reaction: ReactionRecord): Boolean = withContext(dispatchers.io) {
        val database = holder.get()
        database.transactionWithResult {
            if (database.tombstoneQueries.isTombstoned(reaction.eventId, reaction.user).awaitAsOne()) return@transactionWithResult false
            with(reaction) { database.reactionQueries.insertReaction(eventId, postId, user, type, createdAt) } > 0
        }
    }

    override suspend fun reactionIds(postId: String, user: String, type: String): List<String> = withContext(dispatchers.io) {
        holder.get().reactionQueries.selectReactionIds(postId, user, type).awaitAsList()
    }

    override suspend fun delete(eventIds: Collection<String>, user: String) = withContext(dispatchers.io) {
        if (eventIds.isEmpty()) return@withContext
        val database = holder.get()
        database.transaction {
            database.reactionQueries.deleteReactions(eventIds, user)
            eventIds.forEach { database.tombstoneQueries.insertTombstone(it, user) }
        }
    }
}
