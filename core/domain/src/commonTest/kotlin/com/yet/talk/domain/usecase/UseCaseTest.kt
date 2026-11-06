package com.yet.talk.domain.usecase

import androidx.paging.PagingData
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.model.media.MediaUpload
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.PostStats
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.post.Revision
import com.yet.talk.domain.model.post.ThreadReply
import com.yet.talk.domain.model.post.ViewerState
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.Feed
import com.yet.talk.domain.repository.MediaRepository
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.repository.ReactionRepository
import com.yet.talk.domain.usecase.media.UploadImage
import com.yet.talk.domain.usecase.post.PublishPost
import com.yet.talk.domain.usecase.post.ReplyToPost
import com.yet.talk.domain.usecase.social.ToggleLike
import com.yet.talk.domain.usecase.social.ToggleRepost
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest

class UseCaseTest {

    private class RecordingPosts : PostRepository {
        val published = mutableListOf<String>()
        val replies = mutableListOf<Pair<PostId, String>>()

        override suspend fun publish(content: String, images: List<Image>, quoteOf: PostId?): Result<PostId> {
            published += content
            return Result.success(PostId("new"))
        }

        override suspend fun reply(parent: PostId, content: String, images: List<Image>): Result<PostId> {
            replies += parent to content
            return Result.success(PostId("reply"))
        }

        override fun feed(feed: Feed): Flow<PagingData<Post>> = error("unused")
        override fun observePost(id: PostId): Flow<Post?> = error("unused")
        override suspend fun refreshPost(id: PostId): Result<Unit> = error("unused")
        override fun observeThread(id: PostId): Flow<List<ThreadReply>> = error("unused")
        override suspend fun refreshThread(id: PostId): Result<Unit> = error("unused")
        override fun observeRevisions(id: PostId): Flow<List<Revision>> = error("unused")
        override suspend fun refreshRevisions(id: PostId): Result<Unit> = error("unused")
        override suspend fun edit(id: PostId, content: String, images: List<Image>): Result<Unit> = error("unused")
        override suspend fun delete(id: PostId): Result<Unit> = error("unused")
        override suspend fun retryPublish(id: PostId): Result<Unit> = error("unused")
    }

    @Test
    fun publishSanitizesBeforeWriting() = runTest {
        val posts = RecordingPosts()
        PublishPost(posts)("hello\r\nworld\u0000").getOrThrow()
        assertEquals(listOf("hello\nworld"), posts.published)
    }

    @Test
    fun invalidContentNeverReachesTheRepository() = runTest {
        val posts = RecordingPosts()
        assertEquals(DomainError.EmptyContent, PublishPost(posts)("   ").exceptionOrNull())
        assertEquals(DomainError.ContentTooLong(5_000), ReplyToPost(posts)(PostId("p"), "a".repeat(5_001)).exceptionOrNull())
        assertEquals(DomainError.TooManyImages(4), PublishPost(posts)("ok", List(5) { Image("u$it") }).exceptionOrNull())
        assertTrue(posts.published.isEmpty() && posts.replies.isEmpty())
    }

    @Test
    fun togglesFlipTheViewerState() = runTest {
        val calls = mutableListOf<Pair<String, Boolean>>()
        val reactions = object : ReactionRepository {
            override suspend fun setLiked(postId: PostId, liked: Boolean) = Result.success(Unit).also { calls += "like" to liked }
            override suspend fun setReposted(postId: PostId, reposted: Boolean) = Result.success(Unit).also { calls += "repost" to reposted }
        }
        val liked = post(ViewerState(liked = true))

        ToggleLike(reactions)(liked)
        ToggleRepost(reactions)(liked)

        assertEquals(listOf("like" to false, "repost" to true), calls)
    }

    @Test
    fun uploadRejectsUnsupportedFilesWithoutNetwork() = runTest {
        var uploads = 0
        val media = object : MediaRepository {
            override suspend fun upload(file: ImageFile): Result<MediaUpload> {
                uploads++
                return Result.success(MediaUpload("u", file.mimeType, "x", file.bytes.size.toLong()))
            }
        }
        assertTrue(UploadImage(media)(ImageFile(ByteArray(1), "text/html")).isFailure)
        assertTrue(UploadImage(media)(ImageFile(ByteArray(1), "image/webp")).isSuccess)
        assertEquals(1, uploads)
    }

    private fun post(viewer: ViewerState) = Post(
        id = PostId("p"), author = User(UserId("a"), "npub"), content = "x", images = emptyList(),
        createdAt = Instant.fromEpochSeconds(0), editedAt = null, revision = 1, revisionCount = 1,
        thread = null, quoteOf = null, stats = PostStats(), viewer = viewer, publishState = PublishState.Published,
    )
}
