package com.yet.talk.domain.repository

import com.yet.talk.domain.model.Pager
import com.yet.talk.domain.model.post.Post

/** Ranked by [com.yet.talk.domain.rule.TrendingRanker]; the cursor is the rank. */
interface TrendingRepository {
    fun trending(): Pager<Post>
}
