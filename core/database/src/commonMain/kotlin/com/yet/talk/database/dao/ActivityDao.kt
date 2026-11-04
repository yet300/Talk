package com.yet.talk.database.dao

import androidx.paging.PagingSource
import com.yet.talk.database.model.ActivityRecord

/** Replies, quotes, likes and reposts of [me]'s posts and new followers, newest first. */
interface ActivityDao {
    suspend fun pages(me: String): () -> PagingSource<Int, ActivityRecord>
}
