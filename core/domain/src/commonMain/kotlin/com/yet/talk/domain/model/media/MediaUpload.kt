package com.yet.talk.domain.model.media

data class MediaUpload(
    val url: String,
    val mimeType: String,
    val sha256: String,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
)
