package com.yet.talk.domain.model.post

data class Image(
    val url: String,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val alt: String? = null,
)
