package com.yet.talk.database.model

data class NewPostRecord(
    val id: String,
    val author: String,
    val rootId: String?,
    val parentId: String?,
    val quoteId: String?,
    val createdAt: Long,
    val content: String,
    val imagesJson: String,
    val publishState: String,
    val eventJson: String,
)
