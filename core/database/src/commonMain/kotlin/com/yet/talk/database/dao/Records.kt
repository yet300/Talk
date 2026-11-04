package com.yet.talk.database.dao

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

data class RevisionRecord(
    val id: String,
    val postId: String,
    val number: Int,
    val content: String,
    val imagesJson: String,
    val createdAt: Long,
)

data class ProfileRecord(
    val pubkey: String,
    val name: String?,
    val displayName: String?,
    val about: String?,
    val picture: String?,
    val website: String?,
    val nip05: String?,
    val updatedAt: Long,
)

data class ReactionRecord(
    val eventId: String,
    val postId: String,
    val user: String,
    val type: String,
    val createdAt: Long,
)

data class RelayRecord(val url: String, val read: Boolean, val write: Boolean)

data class ActivityRecord(
    val type: String,
    val actor: String,
    val createdAt: Long,
    val eventPostId: String?,
    val subjectPostId: String?,
)

data class TrendingCandidateRecord(val postId: String, val author: String, val createdAt: Long)

data class EngagementRecord(val postId: String, val actor: String, val type: String)

data class TrendingRecord(val rank: Int, val postId: String, val score: Double)

data class SyncStateRecord(val endReached: Boolean, val syncedAt: Long)

/** Stored string values. */
object PublishStates {
    const val PUBLISHED = "published"
    const val PENDING = "pending"
    const val FAILED = "failed"
}

object ReactionTypes {
    const val LIKE = "like"
    const val REPOST = "repost"
}

/** Which list of posts to read. */
sealed interface PostFeed {
    data object Latest : PostFeed

    data object Following : PostFeed

    data class ByAuthor(val author: String, val replies: Boolean) : PostFeed

    data object Bookmarks : PostFeed

    data object Trending : PostFeed
}
