package com.yet.talk.domain.model.video

import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.user.User
import kotlin.time.Instant

/** A video from the HackerTube catalog (data ODC-By 1.0), or a YouTube video outside it (only title and channel). */
data class Video(
    val id: String,
    val title: String,
    val channel: String,
    val summary: String? = null,
    /** "Why Hacker News recommends it". */
    val whyHn: String? = null,
    val quote: String? = null,
    val recommenders: Long = 0,
    /** One of [VideoCatalog.TYPES]. */
    val type: String? = null,
    /** One of [VideoCatalog.LEVELS]. */
    val level: String? = null,
    val primaryTopic: String? = null,
    val categories: List<String> = emptyList(),
) {
    val url: String get() = "https://www.youtube.com/watch?v=$id"

    /** `hqdefault` for cards, `mqdefault` for small ones. */
    fun thumbnail(small: Boolean = false) = "https://i.ytimg.com/vi/$id/${if (small) "mqdefault" else "hqdefault"}.jpg"
}

/** Browse filters; null matches everything. */
data class VideoFilters(val category: String? = null, val level: String? = null, val type: String? = null)

/** The viewer's lists, kept on the device. */
enum class VideoList { Liked, WatchLater, History }

data class VideoViewer(val liked: Boolean = false, val watchLater: Boolean = false)

data class VideoStats(val likes: Long = 0, val comments: Long = 0)

/** A NIP-22 comment on a video. */
data class VideoComment(
    val id: String,
    val author: User,
    val content: String,
    val createdAt: Instant,
    val publishState: PublishState,
)

/** The catalog's fixed vocabularies (HackerTube `level` and `content_type`). */
object VideoCatalog {
    val LEVELS = listOf("intro", "intermediate", "advanced", "general")
    val TYPES = listOf(
        "course", "lecture", "talk", "tutorial", "explainer", "documentary", "interview", "podcast", "demo", "commentary",
        "news", "music", "comedy", "other",
    )
    const val CREDIT_URL = "https://hackertube.org"
}
