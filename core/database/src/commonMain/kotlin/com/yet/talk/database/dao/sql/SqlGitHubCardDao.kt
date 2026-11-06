package com.yet.talk.database.dao.sql

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.GitHubCardDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext

@Inject
internal class SqlGitHubCardDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : GitHubCardDao {

    override suspend fun get(refKey: String): Pair<String?, Long>? = withContext(dispatchers.io) {
        holder.get().gitHubCardQueries.selectCard(refKey) { json, fetchedAt -> json to fetchedAt }.awaitAsOneOrNull()
    }

    override suspend fun put(refKey: String, json: String?, fetchedAt: Long) = withContext(dispatchers.io) {
        holder.get().gitHubCardQueries.putCard(refKey, json, fetchedAt)
        Unit
    }
}
