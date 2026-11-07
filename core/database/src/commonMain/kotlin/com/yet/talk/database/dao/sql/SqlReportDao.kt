package com.yet.talk.database.dao.sql

import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.ReportDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
internal class SqlReportDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : ReportDao {

    override suspend fun insert(eventId: String, reporter: String, targetPubkey: String, targetEvent: String?, type: String, createdAt: Long) =
        withContext(dispatchers.io) {
            holder.get().reportQueries.insertReport(eventId, reporter, targetPubkey, targetEvent, type, createdAt)
            Unit
        }

    override fun observeHiddenPosts(viewer: String): Flow<List<String>> =
        holder.observeList(dispatchers.io) { reportQueries.selectHiddenPosts(viewer) }
}
