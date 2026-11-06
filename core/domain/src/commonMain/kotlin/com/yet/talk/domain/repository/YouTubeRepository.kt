package com.yet.talk.domain.repository

import com.yet.talk.domain.model.post.YouTubeVideo
import kotlinx.coroutines.flow.Flow

interface YouTubeRepository {
    /** The video's title and channel (oEmbed), once loaded; null while loading or when YouTube has none. */
    fun video(videoId: String): Flow<YouTubeVideo?>
}
