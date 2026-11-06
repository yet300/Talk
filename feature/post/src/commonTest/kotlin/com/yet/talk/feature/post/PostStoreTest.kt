package com.yet.talk.feature.post

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.ThreadReply
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.BookmarkRepository
import com.yet.talk.domain.repository.Feed
import com.yet.talk.domain.repository.FollowRepository
import com.yet.talk.domain.usecase.social.ToggleFollow
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.repository.ReactionRepository
import com.yet.talk.domain.usecase.post.ReplyToPost
import com.yet.talk.domain.usecase.social.ToggleBookmark
import com.yet.talk.domain.usecase.social.ToggleLike
import com.yet.talk.domain.usecase.social.ToggleRepost
import com.yet.talk.feature.post.component.post.store.PostStore
import com.yet.talk.feature.post.component.post.store.PostStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PostStoreTest {

    private class FakePosts : PostRepository {
        val replies = mutableListOf<Pair<PostId, String>>()
        var deleteResult: Result<Unit> = Result.success(Unit)

        override fun feed(feed: Feed) = error("unused")
        override fun observePost(id: PostId) = flowOf(null)
        override suspend fun refreshPost(id: PostId) = Result.success(Unit)
        override fun observeThread(id: PostId) = flowOf(emptyList<ThreadReply>())
        override suspend fun refreshThread(id: PostId) = Result.success(Unit)
        override fun observeRevisions(id: PostId) = error("unused")
        override suspend fun refreshRevisions(id: PostId) = Result.success(Unit)
        override suspend fun publish(content: String, images: List<Image>, quoteOf: PostId?) = error("unused")
        override suspend fun reply(parent: PostId, content: String, images: List<Image>): Result<PostId> {
            replies += parent to content
            return Result.success(PostId("r"))
        }
        override suspend fun edit(id: PostId, content: String, images: List<Image>) = error("unused")
        override suspend fun delete(id: PostId) = deleteResult
        override suspend fun retryPublish(id: PostId): Result<Unit> = error("unused")
    }

    private object Accounts : AccountRepository {
        override val account = MutableStateFlow<Account?>(Account(UserId("me"), "npub1me"))
        override suspend fun generateKey() = error("unused")
        override suspend fun importKey(secretKey: String) = error("unused")
        override suspend fun exportSecretKey() = error("unused")
        override suspend fun signOut() = Unit
    }

    private object NoReactions : ReactionRepository {
        override suspend fun setLiked(postId: PostId, liked: Boolean) = Result.success(Unit)
        override suspend fun setReposted(postId: PostId, reposted: Boolean) = Result.success(Unit)
    }

    private object NoBookmarks : BookmarkRepository {
        override suspend fun setBookmarked(postId: PostId, bookmarked: Boolean) = Result.success(Unit)
        override fun bookmarks() = error("unused")
    }

    private object NoFollows : FollowRepository {
        override fun observeIsFollowing(id: UserId) = flowOf(false)
        override suspend fun setFollowing(id: UserId, following: Boolean) = Result.success(Unit)
    }

    private val posts = FakePosts()
    private val store by lazy {
        PostStoreFactory(
            DefaultStoreFactory(), posts, Accounts, ReplyToPost(posts),
            ToggleLike(NoReactions), ToggleRepost(NoReactions), ToggleBookmark(NoBookmarks), NoFollows, ToggleFollow(NoFollows),
        ).create(PostId("p"))
    }

    @BeforeTest
    fun setUp() {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aSentReplyClearsTheBoxAndABlankOneIsNotSent() = runTest {
        assertEquals(UserId("me"), store.state.viewer)
        store.accept(PostStore.Intent.SendReply)
        store.accept(PostStore.Intent.ChangeReply("Nice"))
        store.accept(PostStore.Intent.SendReply)

        assertEquals(listOf(PostId("p") to "Nice"), posts.replies)
        assertEquals("", store.state.replyText)
    }

    @Test
    fun deletingLeavesThePostOnlyWhenRelaysTookIt() = runTest {
        val labels = mutableListOf<PostStore.Label>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { store.labels.collect { labels += it } }

        posts.deleteResult = Result.failure(DomainError.NoRelayAccepted)
        store.accept(PostStore.Intent.Delete)
        assertEquals(DomainError.NoRelayAccepted, store.state.error)
        assertEquals(emptyList<PostStore.Label>(), labels)

        posts.deleteResult = Result.success(Unit)
        store.accept(PostStore.Intent.Delete)
        assertEquals(listOf<PostStore.Label>(PostStore.Label.Deleted), labels)
    }
}
