package com.yet.talk.database.dao.sql

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.ReactionDao
import com.yet.talk.database.model.ReactionRecord
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext

@ContributesBinding(AppScope::class)
@Inject
class SqlReactionDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : ReactionDao {

    override suspend fun insert(reaction: ReactionRecord): Boolean = withContext(dispatchers.io) {
        val database = holder.get()
        database.transactionWithResult {
            if (database.socialQueries.isTombstoned(reaction.eventId, reaction.user).awaitAsOne()) return@transactionWithResult false
            with(reaction) { database.socialQueries.insertReaction(eventId, postId, user, type, createdAt) } > 0
        }
    }

    override suspend fun reactionIds(postId: String, user: String, type: String): List<String> = withContext(dispatchers.io) {
        holder.get().socialQueries.selectReactionIds(postId, user, type).awaitAsList()
    }

    override suspend fun delete(eventIds: Collection<String>, user: String) = withContext(dispatchers.io) {
        if (eventIds.isEmpty()) return@withContext
        val database = holder.get()
        database.transaction {
            database.socialQueries.deleteReactions(eventIds, user)
            eventIds.forEach { database.socialQueries.insertTombstone(it, user) }
        }
    }
}
