package com.yet.talk.domain.repository

import com.yet.talk.domain.model.Pager
import com.yet.talk.domain.model.activity.ActivityItem

interface ActivityRepository {
    /** Replies, quotes, reposts and likes of the viewer's posts, and new followers, newest first. */
    fun activity(): Pager<ActivityItem>
}
