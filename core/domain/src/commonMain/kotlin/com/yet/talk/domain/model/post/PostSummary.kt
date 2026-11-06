package com.yet.talk.domain.model.post

/** What a post card shows: derived from the markdown, so posts need no extra fields. */
data class PostSummary(
    val title: String,
    val excerpt: String,
    val tags: List<String>,
    val readMinutes: Int,
    /** A post starting with a `# heading` is an article card; any other is a text card (`PostCard`). */
    val isArticle: Boolean = false,
    /** The text card's body: the markdown as plain text, paragraphs kept, images left out. */
    val text: String = "",
    /** The link card below the text, if any. */
    val link: PostLink? = null,
)
