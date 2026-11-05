package com.yet.talk.feature.compose

import com.yet.talk.domain.model.post.PostId

/** What the editor writes: KarpathyTalk's New Post, Reply, Quote Post and Edit Post. */
sealed interface ComposeMode {
    data object New : ComposeMode

    data class Reply(val parent: PostId) : ComposeMode

    data class Quote(val quoted: PostId) : ComposeMode

    data class Edit(val post: PostId) : ComposeMode

    /** The post the editor works on (the parent, the quoted or the edited post); null for a new post. */
    val target: PostId?
        get() = when (this) {
            New -> null
            is Reply -> parent
            is Quote -> quoted
            is Edit -> post
        }
}
