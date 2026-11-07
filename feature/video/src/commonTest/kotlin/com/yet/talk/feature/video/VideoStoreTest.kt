package com.yet.talk.feature.video

import androidx.paging.PagingData
import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.video.Video
import com.yet.talk.domain.model.video.VideoComment
import com.yet.talk.domain.model.video.VideoFilters
import com.yet.talk.domain.model.video.VideoList
import com.yet.talk.domain.model.video.VideoStats
import com.yet.talk.domain.model.video.VideoViewer
import com.yet.talk.domain.repository.VideoRepository
import com.yet.talk.feature.video.store.VideoStore
import com.yet.talk.feature.video.store.VideoStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoStoreTest {

    private class FakeVideos : VideoRepository {
        val lists = MutableStateFlow(mapOf<VideoList, Set<String>>())
        val comments = mutableListOf<String>()
        override suspend fun ensureCatalog() = Result.success(Unit)
        override fun categories(): Flow<List<String>> = flowOf(emptyList())
        override fun browse(filters: VideoFilters): Flow<PagingData<Video>> = flowOf(PagingData.empty())
        override fun forYou(limit: Int): Flow<List<Video>> = flowOf(emptyList())
        override fun list(list: VideoList): Flow<List<Video>> = flowOf(emptyList())
        override fun observeVideo(id: String): Flow<Video?> = flowOf(Video(id, "Micrograd", "Karpathy"))
        override fun related(id: String, limit: Int): Flow<List<Video>> = flowOf(listOf(Video("b", "Makemore", "Karpathy")))
        override fun observeViewer(id: String): Flow<VideoViewer> =
            lists.map { VideoViewer(liked = id in it[VideoList.Liked].orEmpty(), watchLater = id in it[VideoList.WatchLater].orEmpty()) }
        override fun observeStats(id: String): Flow<VideoStats> = flowOf(VideoStats())
        override fun comments(id: String): Flow<List<VideoComment>> = flowOf(emptyList())
        override suspend fun refresh(id: String) = Result.success(Unit)
        override suspend fun setLiked(id: String, liked: Boolean) = Result.success(Unit).also { toggle(VideoList.Liked, id, liked) }
        override suspend fun setWatchLater(id: String, on: Boolean) = toggle(VideoList.WatchLater, id, on)
        override suspend fun markWatched(id: String) = toggle(VideoList.History, id, true)
        override suspend fun comment(id: String, content: String) =
            if (content.isBlank()) Result.failure(DomainError.EmptyContent) else Result.success(Unit).also { comments += content }

        private fun toggle(list: VideoList, id: String, on: Boolean) {
            val current = lists.value[list].orEmpty()
            lists.value = lists.value + (list to if (on) current + id else current - id)
        }
    }

    private val videos = FakeVideos()
    private val store by lazy { VideoStoreFactory(DefaultStoreFactory(), videos).create("a") }

    @BeforeTest
    fun setUp() {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun playingAddsToHistoryAndTheListsToggle() = runTest {
        assertEquals("Micrograd" to listOf("b"), store.state.video?.title to store.state.related.map { it.id })
        store.accept(VideoStore.Intent.Play)
        store.accept(VideoStore.Intent.ToggleLike)
        store.accept(VideoStore.Intent.ToggleWatchLater)
        assertEquals(true, store.state.isPlaying)
        assertEquals(VideoViewer(liked = true, watchLater = true), store.state.viewer)
        assertEquals(setOf("a"), videos.lists.value[VideoList.History])
        store.accept(VideoStore.Intent.ToggleLike)
        assertEquals(false, store.state.viewer.liked)
    }

    @Test
    fun aSentCommentClearsTheField() = runTest {
        store.accept(VideoStore.Intent.ChangeComment("great lecture"))
        store.accept(VideoStore.Intent.SendComment)
        assertEquals(listOf("great lecture"), videos.comments)
        assertEquals("", store.state.comment)
    }
}
