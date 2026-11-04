package com.yet.talk.api.source.media

import kotlinx.serialization.Serializable

/** A stored blob (BUD-02 blob descriptor). */
@Serializable
data class BlobDescriptor(
    val url: String,
    val sha256: String,
    val size: Long,
    val type: String? = null,
)
