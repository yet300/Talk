package com.yet.talk.domain.model

/** An image picked by the user, before upload. */
class ImageFile(val bytes: ByteArray, val mimeType: String)

data class MediaUpload(
    val url: String,
    val mimeType: String,
    val sha256: String,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
)

data class Relay(val url: String, val read: Boolean = true, val write: Boolean = true)
