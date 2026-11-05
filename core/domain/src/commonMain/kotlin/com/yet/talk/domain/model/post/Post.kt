package com.yet.talk.domain.model.post

import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.rule.PostSummaries
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
) {
    /** Title, excerpt, tags and reading time for cards; computed once per instance. */
    val summary: PostSummary by lazy { PostSummaries.of(content) }
}
