package com.yet.talk.database.dao.sql

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.SyncDao
import com.yet.talk.database.model.SyncStateRecord
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext

@Inject
internal class SqlSyncDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : SyncDao {

    override suspend fun state(feedKey: String): SyncStateRecord? = withContext(dispatchers.io) {
        holder.get().syncStateQueries.selectSyncState(feedKey) { endReached, cursor, syncedAt ->
            SyncStateRecord(endReached != 0L, cursor, syncedAt)
        }.awaitAsOneOrNull()
    }

    override suspend fun setState(feedKey: String, state: SyncStateRecord) = withContext(dispatchers.io) {
        holder.get().syncStateQueries.setSyncState(feedKey, if (state.endReached) 1 else 0, state.cursor, state.syncedAt)
        Unit
    }
}
