package com.yet.talk.domain.model

import kotlin.time.Instant

data class Post(
    val id: PostId,
    val author: User,
    /** Markdown of the current revision. */
    val content: String,
    val images: List<Image>,
    val createdAt: Instant,
    val editedAt: Instant?,
    /** 1 for an unedited post. */
    val revision: Int,
    val revisionCount: Int,
    /** Null for a top-level post. */
    val thread: ThreadPosition?,
    val quoteOf: PostId?,
    val stats: PostStats,
    val viewer: ViewerState,
    val publishState: PublishState,
)

data class ThreadPosition(val rootId: PostId, val parentId: PostId)

data class Image(
    val url: String,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val alt: String? = null,
)

data class PostStats(val likes: Long = 0, val reposts: Long = 0, val replies: Long = 0)

/** What the signed-in user did to a post. All false when signed out. */
data class ViewerState(val liked: Boolean = false, val reposted: Boolean = false, val bookmarked: Boolean = false)

/** Own writes are shown immediately and confirmed by relays afterwards. */
enum class PublishState { Published, Pending, Failed }

data class Revision(
    val postId: PostId,
    val number: Int,
    val content: String,
    val createdAt: Instant,
)

/** A reply inside a thread; [depth] 0 is a direct reply to the opened post. */
data class ThreadReply(val post: Post, val depth: Int)

data class ActivityItem(
    val type: Type,
    val actor: User,
    val createdAt: Instant,
    /** The actor's reply or quote; null for likes, reposts and follows. */
    val eventPost: Post?,
    /** The viewer's post that was acted on; null for follows. */
    val subjectPost: Post?,
) {
    enum class Type { Reply, Quote, Repost, Like, Follow }
}
