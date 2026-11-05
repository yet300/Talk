package com.yet.talk.database.dao.sql

import androidx.paging.PagingSource
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.ActivityDao
import com.yet.talk.database.model.ActivityRecord
import dev.zacsweers.metro.Inject

@Inject
internal class SqlActivityDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : ActivityDao {

    override suspend fun pages(me: String): () -> PagingSource<Int, ActivityRecord> {
        val queries = holder.get().activityQueries
        return {
            OffsetPagingSource(countQuery = queries.countActivity(me), context = dispatchers.io, pageQuery = { limit: Long, offset: Long ->
                queries.selectActivity(me, limit, offset, ::ActivityRecord)
            })
        }
    }
}
