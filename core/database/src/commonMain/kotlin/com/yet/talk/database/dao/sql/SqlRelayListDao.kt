package com.yet.talk.database.dao.sql

import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.RelayListDao
import com.yet.talk.database.model.RelayRecord
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
internal class SqlRelayListDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : RelayListDao {

    override suspend fun replace(owner: String, relays: List<RelayRecord>, createdAt: Long): Boolean = withContext(dispatchers.io) {
        holder.get().replaceList(owner, KIND_RELAY_LIST, createdAt) {
            relayListQueries.deleteRelays(owner)
            relays.forEach { relayListQueries.insertRelay(owner, it.url, it.read.toLong(), it.write.toLong()) }
        }
    }

    override suspend fun version(owner: String): Long? = withContext(dispatchers.io) {
        holder.get().listVersion(owner, KIND_RELAY_LIST)
    }

    override fun observe(owner: String): Flow<List<RelayRecord>> =
        holder.observeList(dispatchers.io) {
            relayListQueries.selectRelays(owner) { url, read, write -> RelayRecord(url, read != 0L, write != 0L) }
        }

    private fun Boolean.toLong() = if (this) 1L else 0L
}
