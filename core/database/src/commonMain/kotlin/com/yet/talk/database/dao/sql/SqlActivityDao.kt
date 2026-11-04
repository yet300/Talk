package com.yet.talk.database.dao.sql

import androidx.paging.PagingSource
import app.cash.sqldelight.paging3.QueryPagingSource
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.ActivityDao
import com.yet.talk.database.model.ActivityRecord
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class SqlActivityDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : ActivityDao {

    override suspend fun pages(me: String): () -> PagingSource<Int, ActivityRecord> {
        val queries = holder.get().activityQueries
        return {
            QueryPagingSource(countQuery = queries.countActivity(me), transacter = queries, context = dispatchers.io, queryProvider = { limit: Long, offset: Long ->
                queries.selectActivity(me, limit, offset, ::ActivityRecord)
            })
        }
    }
}
