package com.yet.talk.domain.repository

import androidx.paging.PagingData
import com.yet.talk.domain.model.video.Video
import com.yet.talk.domain.model.video.VideoComment
import com.yet.talk.domain.model.video.VideoFilters
import com.yet.talk.domain.model.video.VideoList
import com.yet.talk.domain.model.video.VideoStats
import com.yet.talk.domain.model.video.VideoViewer
import kotlinx.coroutines.flow.Flow

/** The HackerTube catalog in the database, the viewer's lists, and likes and comments on Nostr. */
interface VideoRepository {
    /** Loads the bundled catalog into the database the first time; afterwards does nothing. */
    suspend fun ensureCatalog(): Result<Unit>

    /** Category names, the largest first. */
    fun categories(): Flow<List<String>>

    fun browse(filters: VideoFilters): Flow<PagingData<Video>>

    /** Computed on the device from the Liked and History lists; the most recommended for a new viewer. */
    fun forYou(limit: Int): Flow<List<Video>>

    fun list(list: VideoList): Flow<List<Video>>

    /** A catalog video, or, for any other YouTube video, its title and channel once oEmbed answers; null meanwhile. */
    fun observeVideo(id: String): Flow<Video?>

    fun related(id: String, limit: Int): Flow<List<Video>>

    fun observeViewer(id: String): Flow<VideoViewer>

    fun observeStats(id: String): Flow<VideoStats>

    fun comments(id: String): Flow<List<VideoComment>>

    /** Fetches the video's likes and comments from relays. */
    suspend fun refresh(id: String): Result<Unit>

    /** Liked is a device list and a public NIP-25 reaction; unliking deletes the reaction (NIP-09). */
    suspend fun setLiked(id: String, liked: Boolean): Result<Unit>

    suspend fun setWatchLater(id: String, on: Boolean)

    /** Adds the video to History (on Play). */
    suspend fun markWatched(id: String)

    /** Publishes a comment (NIP-22) on the video. */
    suspend fun comment(id: String, content: String): Result<Unit>
}
