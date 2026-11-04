package com.yet.talk.database.model

data class ActivityRecord(
    val type: String,
    val actor: String,
    val createdAt: Long,
    val eventPostId: String?,
    val subjectPostId: String?,
)
