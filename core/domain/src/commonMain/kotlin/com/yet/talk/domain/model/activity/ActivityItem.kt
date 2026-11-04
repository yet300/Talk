package com.yet.talk.domain.model.activity

import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.user.User
import kotlin.time.Instant

data class ActivityItem(
    val type: Type,
    val actor: User,
    val createdAt: Instant,
    /** The actor's reply or quote; null for likes, reposts and follows. */
    val eventPost: Post?,
    /** The viewer's post that was acted on; null for follows. */
    val subjectPost: Post?,
) {
    enum class Type { Reply, Quote, Repost, Like, Follow }
}
