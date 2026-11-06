package com.yet.talk.domain.model.post

import com.yet.talk.domain.model.github.GitHubRef

/** The one link a post card shows below its text (`PostCard`): YouTube, GitHub, or any other as a row. */
sealed interface PostLink {
    val url: String

    data class YouTube(val videoId: String, override val url: String) : PostLink {
        /** `hqdefault` for cards, `mqdefault` for small ones (decision 11). */
        fun thumbnail(small: Boolean = false) = "https://i.ytimg.com/vi/$videoId/${if (small) "mqdefault" else "hqdefault"}.jpg"
    }

    data class GitHub(val ref: GitHubRef) : PostLink {
        override val url: String get() = ref.url
    }

    data class Web(override val url: String) : PostLink
}
