package com.yet.talk.database.dao.sql

import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.MaintenanceDao
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext

@ContributesBinding(AppScope::class)
@Inject
class SqlMaintenanceDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : MaintenanceDao {
    override suspend fun clearUserData() = withContext(dispatchers.io) {
        // Grouped statements return a lazy AsyncValue: nothing runs until it is awaited.
        holder.get().maintenanceQueries.clearUserData().await()
        Unit
    }
}
