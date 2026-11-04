package com.yet.talk.database.model

/** A post as stored, joined with its author's profile, counters and the viewer's flags. Times are epoch seconds. */
data class PostRecord(
    val id: String,
    val author: String,
    val rootId: String?,
    val parentId: String?,
    val quoteId: String?,
    val createdAt: Long,
    val content: String,
    val imagesJson: String,
    val revision: Int,
    val revisionCount: Int,
    val editedAt: Long?,
    val publishState: String,
    val authorName: String?,
    val authorDisplayName: String?,
    val authorPicture: String?,
    val authorNip05: String?,
    val likeCount: Long,
    val repostCount: Long,
    val replyCount: Long,
    val viewerLiked: Boolean,
    val viewerReposted: Boolean,
    val viewerBookmarked: Boolean,
    /** Thread depth; 0 outside thread queries. */
    val depth: Int = 0,
)
