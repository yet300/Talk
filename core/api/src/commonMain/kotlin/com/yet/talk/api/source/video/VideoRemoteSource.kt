package com.yet.talk.api.source.video

import com.yet.talk.api.schema.TalkEvent

/** Likes and comments on a video URL, from relays. */
interface VideoRemoteSource {
    /** [TalkEvent.UrlLike]s and [TalkEvent.UrlComment]s on [videoUrl]. */
    suspend fun activity(videoUrl: String): List<TalkEvent>
}
