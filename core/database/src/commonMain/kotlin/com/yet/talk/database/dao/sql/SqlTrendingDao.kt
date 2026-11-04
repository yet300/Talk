package com.yet.talk.database.dao.sql

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.TrendingDao
import com.yet.talk.database.model.EngagementRecord
import com.yet.talk.database.model.TrendingCandidateRecord
import com.yet.talk.database.model.TrendingRecord
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext

@ContributesBinding(AppScope::class)
@Inject
class SqlTrendingDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : TrendingDao {

    override suspend fun candidates(since: Long): List<TrendingCandidateRecord> = withContext(dispatchers.io) {
        holder.get().trendingQueries.selectCandidates(since, ::TrendingCandidateRecord).awaitAsList()
    }

    override suspend fun engagements(since: Long): List<EngagementRecord> = withContext(dispatchers.io) {
        holder.get().trendingQueries.selectEngagements(since) { postId, actor, type ->
            EngagementRecord(postId!!, actor, type)
        }.awaitAsList()
    }

    override suspend fun replace(entries: List<TrendingRecord>, computedAt: Long) = withContext(dispatchers.io) {
        val database = holder.get()
        database.transaction {
            database.trendingQueries.deleteTrending()
            entries.forEach { database.trendingQueries.insertTrending(it.rank.toLong(), it.postId, it.score, computedAt) }
        }
    }

    override suspend fun computedAt(): Long? = withContext(dispatchers.io) {
        holder.get().trendingQueries.selectComputedAt().awaitAsOneOrNull()?.MAX
    }
}
