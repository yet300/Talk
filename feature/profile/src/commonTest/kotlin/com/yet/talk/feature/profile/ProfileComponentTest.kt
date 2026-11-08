package com.yet.talk.feature.profile

import androidx.paging.PagingData
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.account.Account
import com.yet.talk.domain.model.account.AppLock
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.Revision
import com.yet.talk.domain.model.post.ThreadReply
import com.yet.talk.domain.model.user.ProfileEdit
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.model.user.UserStats
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.BookmarkRepository
import com.yet.talk.domain.repository.Feed
import com.yet.talk.domain.repository.FollowRepository
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.domain.repository.ReactionRepository
import com.yet.talk.domain.usecase.social.ToggleBookmark
import com.yet.talk.domain.usecase.social.ToggleFollow
import com.yet.talk.domain.usecase.social.ToggleLike
import com.yet.talk.domain.usecase.social.ToggleRepost
import com.yet.talk.feature.profile.store.ProfileStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The profile's tabs are pages: the selection lives there only, and Bookmarks exists only on your own profile. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileComponentTest {

    private val me = UserId("me")
    private val someone = UserId("someone")
    private val account = MutableStateFlow<Account?>(Account(me, "npub1me"))

    @BeforeTest
    fun setUp() {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun someoneElsesProfileHasPostsAndRepliesAndATabTapSelectsItsPage() {
        val profile = profile(someone)

        assertEquals(listOf(ProfileComponent.Tab.Posts, ProfileComponent.Tab.Replies), profile.tabs())
        assertEquals(ProfileComponent.Tab.Posts, profile.model.value.tab)

        profile.onTabSelected(ProfileComponent.Tab.Replies)
        assertEquals(ProfileComponent.Tab.Replies, profile.model.value.tab)
        assertEquals(1, profile.pages.value.selectedIndex)

        // Bookmarks is the viewer's own list: not a tab here.
        profile.onTabSelected(ProfileComponent.Tab.Bookmarks)
        assertEquals(ProfileComponent.Tab.Replies, profile.model.value.tab)
    }

    @Test
    fun yourOwnProfileHasBookmarks() {
        val profile = profile(null)

        assertEquals(ProfileComponent.Tab.entries, profile.tabs())
        profile.onTabSelected(ProfileComponent.Tab.Bookmarks)
        assertEquals(ProfileComponent.Tab.Bookmarks, profile.model.value.tab)
    }

    @Test
    fun bookmarksJoinsOnceTheProfileTurnsOutToBeYoursAndTheSelectedTabStays() {
        account.value = null
        val profile = profile(me)
        profile.onTabSelected(ProfileComponent.Tab.Replies)
        assertEquals(listOf(ProfileComponent.Tab.Posts, ProfileComponent.Tab.Replies), profile.tabs())

        account.value = Account(me, "npub1me")

        assertEquals(ProfileComponent.Tab.entries, profile.tabs())
        assertEquals(ProfileComponent.Tab.Replies, profile.model.value.tab)
    }

    @Test
    fun bookmarksLeavesWhenYouSignOutAndTheSelectionFallsBackToPosts() {
        val profile = profile(me)
        profile.onTabSelected(ProfileComponent.Tab.Bookmarks)

        account.value = Account(someone, "npub1someone")

        assertEquals(listOf(ProfileComponent.Tab.Posts, ProfileComponent.Tab.Replies), profile.tabs())
        assertEquals(ProfileComponent.Tab.Posts, profile.model.value.tab)
    }

    private fun ProfileComponent.tabs() = pages.value.items.map { it.configuration }

    private fun profile(userId: UserId?): ProfileComponent {
        val follows = FakeFollows()
        val reactions = FakeReactions()
        val bookmarks = FakeBookmarks()
        return DefaultProfileComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry().apply { resume() }),
            userId = userId,
            output = {},
            storeFactory = ProfileStoreFactory(
                storeFactory = DefaultStoreFactory(),
                accounts = FakeAccounts(account),
                profiles = FakeProfiles(),
                follows = follows,
                toggleFollow = ToggleFollow(follows),
                toggleLike = ToggleLike(reactions),
                toggleRepost = ToggleRepost(reactions),
                toggleBookmark = ToggleBookmark(bookmarks),
            ),
            postRepository = FakePosts(),
            bookmarkRepository = bookmarks,
        )
    }

    private class FakeAccounts(override val account: Flow<Account?>) : AccountRepository {
        override val lock: Flow<AppLock> = flowOf(AppLock())
        override suspend fun generateKey(): Result<String> = error("unused")
        override suspend fun importKey(secretKey: String): Result<Account> = error("unused")
        override suspend fun exportSecretKey(): Result<String> = error("unused")
        override suspend fun signOut() = Unit
        override suspend fun unlock(passcode: String): Result<Unit> = error("unused")
        override suspend fun setPasscode(passcode: String): Result<Unit> = error("unused")
        override suspend fun removePasscode(): Result<Unit> = error("unused")
    }

    private class FakeProfiles : ProfileRepository {
        override fun observeUser(id: UserId): Flow<User> = flowOf(User(id, "npub1${id.value}"))
        override fun observeStats(id: UserId): Flow<UserStats> = flowOf(UserStats())
        override suspend fun refresh(id: UserId): Result<Unit> = Result.success(Unit)
        override suspend fun updateMyProfile(edit: ProfileEdit): Result<Unit> = error("unused")
        override suspend fun verifyNip05(id: UserId, identifier: String): Result<Boolean> = error("unused")
    }

    private class FakeFollows : FollowRepository {
        override fun observeIsFollowing(id: UserId): Flow<Boolean> = flowOf(false)
        override suspend fun setFollowing(id: UserId, following: Boolean): Result<Unit> = Result.success(Unit)
    }

    private class FakeReactions : ReactionRepository {
        override suspend fun setLiked(postId: PostId, liked: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setReposted(postId: PostId, reposted: Boolean): Result<Unit> = Result.success(Unit)
    }

    private class FakeBookmarks : BookmarkRepository {
        override suspend fun setBookmarked(postId: PostId, bookmarked: Boolean): Result<Unit> = Result.success(Unit)
        override fun bookmarks(): Flow<PagingData<Post>> = flowOf(PagingData.empty())
    }

    private class FakePosts : PostRepository {
        override fun feed(feed: Feed): Flow<PagingData<Post>> = flowOf(PagingData.empty())
        override fun observePost(id: PostId): Flow<Post?> = emptyFlow()
        override suspend fun refreshPost(id: PostId): Result<Unit> = error("unused")
        override fun observeThread(id: PostId): Flow<List<ThreadReply>> = emptyFlow()
        override suspend fun refreshThread(id: PostId): Result<Unit> = error("unused")
        override fun observeRevisions(id: PostId): Flow<List<Revision>> = emptyFlow()
        override suspend fun refreshRevisions(id: PostId): Result<Unit> = error("unused")
        override suspend fun publish(content: String, images: List<Image>, quoteOf: PostId?): Result<PostId> = error("unused")
        override suspend fun reply(parent: PostId, content: String, images: List<Image>): Result<PostId> = error("unused")
        override suspend fun edit(id: PostId, content: String, images: List<Image>): Result<Unit> = error("unused")
        override suspend fun delete(id: PostId): Result<Unit> = error("unused")
        override suspend fun retryPublish(id: PostId): Result<Unit> = error("unused")
    }
}
