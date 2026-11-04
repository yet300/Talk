package com.yet.talk.domain.model.post

import kotlin.time.Instant

data class Revision(
    val postId: PostId,
    val number: Int,
    val content: String,
    val createdAt: Instant,
)
