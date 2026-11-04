package com.yet.talk.database.dao

import com.yet.talk.database.model.EngagementRecord
import com.yet.talk.database.model.TrendingCandidateRecord
import com.yet.talk.database.model.TrendingRecord

interface TrendingDao {
    suspend fun candidates(since: Long): List<TrendingCandidateRecord>

    suspend fun engagements(since: Long): List<EngagementRecord>

    suspend fun replace(entries: List<TrendingRecord>, computedAt: Long)

    suspend fun computedAt(): Long?
}
