package com.yet.talk.feature.tab.home

import androidx.paging.PagingData
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.Revision
import com.yet.talk.domain.model.post.ThreadReply
import com.yet.talk.domain.repository.BookmarkRepository
import com.yet.talk.domain.repository.Feed
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.repository.ReactionRepository
import com.yet.talk.domain.repository.TrendingRepository
import com.yet.talk.domain.usecase.social.ToggleBookmark
import com.yet.talk.domain.usecase.social.ToggleLike
import com.yet.talk.domain.usecase.social.ToggleRepost
import com.yet.talk.feature.tab.home.store.HomeStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Following, Popular and Latest are pages: the selected feed lives there only, a tab tap and a swipe alike. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeComponentTest {

    @BeforeTest
    fun setUp() {
        isAssertOnMainThreadEnabled = false
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsOnFollowingAndATabSelectsItsPage() {
        val home = home()
        assertEquals(HomeComponent.Feed.entries, home.pages.value.items.map { it.configuration })
        assertEquals(HomeComponent.Feed.Following, home.model.value.feed)

        home.onFeedSelected(HomeComponent.Feed.Latest)

        assertEquals(2, home.pages.value.selectedIndex)
        assertEquals(HomeComponent.Feed.Latest, home.model.value.feed)
    }

    @Test
    fun eachPageCarriesItsOwnFeed() {
        val home = home()
        home.onFeedSelected(HomeComponent.Feed.Popular)
        val page = home.pages.value.items[home.pages.value.selectedIndex].instance
        assertEquals(HomeComponent.Feed.Popular, page?.feed)
    }

    @Test
    fun theModelFollowsEverySelection() {
        val home = home()
        val seen = mutableListOf<HomeComponent.Feed>()
        home.model.subscribe { seen += it.feed }

        home.onFeedSelected(HomeComponent.Feed.Popular)
        home.onFeedSelected(HomeComponent.Feed.Following)

        assertEquals(HomeComponent.Feed.Following, seen.last())
        assertEquals(true, HomeComponent.Feed.Popular in seen)
    }

    private fun home(): HomeComponent {
        val reactions = FakeReactions()
        return DefaultHomeComponent(
            componentContext = DefaultComponentContext(LifecycleRegistry().apply { resume() }),
            output = {},
            storeFactory = HomeStoreFactory(DefaultStoreFactory(), ToggleLike(reactions), ToggleRepost(reactions), ToggleBookmark(FakeBookmarks())),
            postRepository = FakePosts(),
            trendingRepository = object : TrendingRepository {
                override fun trending(): Flow<PagingData<Post>> = flowOf(PagingData.empty())
            },
        )
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
