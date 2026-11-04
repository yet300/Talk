package com.yet.talk.database.model

data class RevisionRecord(
    val id: String,
    val postId: String,
    val number: Int,
    val content: String,
    val imagesJson: String,
    val createdAt: Long,
)
