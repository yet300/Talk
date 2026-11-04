package com.yet.talk.domain.repository

import androidx.paging.PagingData
import com.yet.talk.domain.model.activity.ActivityItem
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    /** Replies, quotes, reposts and likes of the viewer's posts, and new followers, newest first. */
    fun activity(): Flow<PagingData<ActivityItem>>
}
