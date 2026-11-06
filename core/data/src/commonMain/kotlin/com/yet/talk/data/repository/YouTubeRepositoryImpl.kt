package com.yet.talk.data.repository

import com.yet.talk.api.source.youtube.YouTubeRemoteSource
import com.yet.talk.domain.model.post.YouTubeVideo
import com.yet.talk.domain.repository.YouTubeRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** oEmbed answers are kept for the session; a failed lookup is tried again next time the card shows. */
@SingleIn(AppScope::class)
@Inject
internal class YouTubeRepositoryImpl(private val youtube: YouTubeRemoteSource) : YouTubeRepository {
    private val lock = Mutex()
    private val known = mutableMapOf<String, YouTubeVideo?>()

    override fun video(videoId: String): Flow<YouTubeVideo?> = flow {
        lock.withLock { if (videoId in known) return@flow emit(known[videoId]) }
        emit(null)
        val video = runCatching { youtube.video(videoId) }.getOrElse { return@flow }?.let { YouTubeVideo(videoId, it.title, it.channel) }
        lock.withLock { known[videoId] = video }
        emit(video)
    }
}
