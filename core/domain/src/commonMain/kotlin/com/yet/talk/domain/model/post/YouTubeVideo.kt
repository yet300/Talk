package com.yet.talk.domain.model.post

/** What YouTube's oEmbed says about a linked video. */
data class YouTubeVideo(val videoId: String, val title: String, val channel: String)
