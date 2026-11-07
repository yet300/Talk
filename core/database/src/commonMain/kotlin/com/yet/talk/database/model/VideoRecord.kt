package com.yet.talk.database.model

/** A catalog video as stored. */
data class VideoRecord(
    val vid: String,
    val title: String,
    val channel: String,
    val summary: String?,
    val whyHn: String?,
    val contentType: String?,
    val level: String?,
    val primaryTopic: String?,
    val quote: String?,
    val recommenders: Long,
    val score: Double,
)

/** A comment on a video (NIP-22), with its author's profile. */
data class VideoCommentRecord(
    val id: String,
    val vid: String,
    val author: String,
    val content: String,
    val createdAt: Long,
    val publishState: String,
    val authorName: String?,
    val authorDisplayName: String?,
    val authorPicture: String?,
    val authorNip05: String?,
)

/** The viewer's lists of videos, kept on the device. */
object VideoLists {
    const val LIKED = "liked"
    const val LATER = "later"
    const val HISTORY = "history"
}
