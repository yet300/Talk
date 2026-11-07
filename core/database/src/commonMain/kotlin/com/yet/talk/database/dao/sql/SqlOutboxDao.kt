package com.yet.talk.database.dao.sql

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.OutboxDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
internal class SqlOutboxDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : OutboxDao {

    override suspend fun owe(eventId: String, relay: String, eventJson: String, attempts: Long, nextAt: Long) = withContext(dispatchers.io) {
        holder.get().outboxQueries.upsert(attempts, nextAt, eventId, relay, eventJson).await()
        Unit
    }

    override suspend fun settle(eventId: String, relay: String) = withContext(dispatchers.io) {
        holder.get().outboxQueries.remove(eventId, relay)
        Unit
    }

    override suspend fun relaysFor(eventId: String): Map<String, Long> = withContext(dispatchers.io) {
        holder.get().outboxQueries.relaysFor(eventId) { relay, attempts -> relay to attempts }.awaitAsList().toMap()
    }

    override suspend fun due(now: Long): List<OutboxDao.Entry> = withContext(dispatchers.io) {
        holder.get().outboxQueries.due(now, OutboxDao::Entry).awaitAsList()
    }

    override fun observePending(): Flow<Long> = holder.observeOne(dispatchers.io) { outboxQueries.countPending() }
}
