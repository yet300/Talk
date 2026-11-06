package com.yet.talk.api.source.youtube

/** YouTube's oEmbed endpoint, which answers browsers too (it sends CORS headers). */
fun interface YouTubeRemoteSource {
    /** The title and channel of a video; null when YouTube has no such (public) video. Throws when it cannot answer. */
    suspend fun video(videoId: String): YouTubeOEmbed?
}

data class YouTubeOEmbed(val title: String, val channel: String)
