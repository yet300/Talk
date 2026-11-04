package com.yet.talk.database.model

data class ReactionRecord(
    val eventId: String,
    val postId: String,
    val user: String,
    val type: String,
    val createdAt: Long,
)
