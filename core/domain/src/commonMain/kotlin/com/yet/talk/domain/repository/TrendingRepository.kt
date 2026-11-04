package com.yet.talk.domain.repository

import androidx.paging.PagingData
import com.yet.talk.domain.model.post.Post
import kotlinx.coroutines.flow.Flow

/** Ranked by [com.yet.talk.domain.rule.TrendingRanker], best first. */
interface TrendingRepository {
    fun trending(): Flow<PagingData<Post>>
}
