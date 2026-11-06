package com.yet.talk.feature.compose

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.model.media.MediaUpload
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.PostStats
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.post.ViewerState
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.Feed
import com.yet.talk.domain.repository.MediaRepository
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.usecase.media.UploadImage
import com.yet.talk.domain.usecase.post.EditPost
import com.yet.talk.domain.usecase.post.PublishPost
import com.yet.talk.domain.usecase.post.ReplyToPost
import com.yet.talk.feature.compose.store.ComposeStore
import com.yet.talk.feature.compose.store.ComposeStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant

class ComposeStoreTest {

    private class FakePosts : PostRepository {
        val published = mutableListOf<Pair<String, PostId?>>()
        val replies = mutableListOf<PostId>()
        val edits = mutableListOf<String>()
        val stored = MutableStateFlow<Post?>(null)

        override fun feed(feed: Feed) = error("unused")
        override fun observePost(id: PostId) = stored
        override suspend fun refreshPost(id: PostId) = Result.success(Unit)
        override fun observeThread(id: PostId) = error("unused")
        override suspend fun refreshThread(id: PostId) = Result.success(Unit)
        override fun observeRevisions(id: PostId) = error("unused")
        override suspend fun refreshRevisions(id: PostId) = Result.success(Unit)
        override suspend fun publish(content: String, images: List<Image>, quoteOf: PostId?): Result<PostId> {
            published += content to quoteOf
            return Result.success(PostId("new"))
        }
        override suspend fun reply(parent: PostId, content: String, images: List<Image>): Result<PostId> {
            replies += parent
            return Result.success(PostId("reply"))
        }
        override suspend fun edit(id: PostId, content: String, images: List<Image>): Result<Unit> {
            edits += content
            return Result.success(Unit)
        }
        override suspend fun delete(id: PostId) = Result.success(Unit)
        override suspend fun retryPublish(id: PostId): Result<Unit> = error("unused")
    }

    private object FakeMedia : MediaRepository {
        override suspend fun upload(file: ImageFile) =
            Result.success(MediaUpload(url = "https://blossom.example/a.png", mimeType = file.mimeType, sha256 = "a", sizeBytes = 1))
    }

    private val posts = FakePosts()

    private fun store(mode: ComposeMode, draft: String? = null) =
        ComposeStoreFactory(DefaultStoreFactory(), posts, PublishPost(posts), ReplyToPost(posts), EditPost(posts), UploadImage(FakeMedia))
            .create(mode, draft)

    private fun post(content: String) = Post(
        id = PostId("old"), author = User(UserId("me"), "npub1me"), content = content, images = emptyList(),
        createdAt = Instant.fromEpochSeconds(1), editedAt = null, revision = 1, revisionCount = 1, thread = null,
        quoteOf = null, stats = PostStats(), viewer = ViewerState(), publishState = PublishState.Published,
    )

    @BeforeTest
    fun setUp() {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun publishesAQuoteAndReportsTheNewPost() = runTest {
        val store = store(ComposeMode.Quote(PostId("q")))
        val labels = mutableListOf<ComposeStore.Label>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { store.labels.collect { labels += it } }

        store.accept(ComposeStore.Intent.ChangeText("# Hello"))
        store.accept(ComposeStore.Intent.Publish)

        assertEquals(listOf<Pair<String, PostId?>>("# Hello" to PostId("q")), posts.published)
        assertEquals(listOf<ComposeStore.Label>(ComposeStore.Label.Published(PostId("new"))), labels)
    }

    @Test
    fun anEmptyPostIsRejectedBeforeItIsSent() = runTest {
        val store = store(ComposeMode.New)
        store.accept(ComposeStore.Intent.ChangeText("   "))
        store.accept(ComposeStore.Intent.Publish)

        assertNotNull(store.state.error)
        assertEquals(emptyList<Pair<String, PostId?>>(), posts.published)
    }

    @Test
    fun editingStartsFromThePostOnceAndKeepsALaterDraft() = runTest {
        posts.stored.value = post("original")
        val store = store(ComposeMode.Edit(PostId("old")))
        assertEquals("original", store.state.text)

        store.accept(ComposeStore.Intent.ChangeText("changed"))
        posts.stored.value = post("original").copy(stats = PostStats(likes = 1))
        assertEquals("changed", store.state.text, "a refreshed post does not overwrite what was typed")

        store.accept(ComposeStore.Intent.Publish)
        assertEquals(listOf("changed"), posts.edits)
    }

    @Test
    fun anUploadedImageIsInsertedAsMarkdown() = runTest {
        val store = store(ComposeMode.New)
        store.accept(ComposeStore.Intent.ChangeText("Text"))
        store.accept(ComposeStore.Intent.AttachImage(ImageFile(byteArrayOf(1), "image/png")))

        assertEquals("Text\n\n![](https://blossom.example/a.png)\n", store.state.text)
        assertEquals(1, store.state.images.size)
    }
}
