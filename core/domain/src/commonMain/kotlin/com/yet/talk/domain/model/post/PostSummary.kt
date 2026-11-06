package com.yet.talk.domain.model.post

/** What a post card shows: derived from the markdown, so posts need no extra fields. */
data class PostSummary(
    val title: String,
    val excerpt: String,
    val tags: List<String>,
    val readMinutes: Int,
    /** The first GitHub repository or user linked in the post: it gets a card. */
    val github: com.yet.talk.domain.model.github.GitHubRef? = null,
)
