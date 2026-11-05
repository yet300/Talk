package com.yet.talk.feature.tab.search

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.search.SearchResults
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.BookmarkRepository
import com.yet.talk.domain.repository.ReactionRepository
import com.yet.talk.domain.repository.SearchRepository
import com.yet.talk.domain.usecase.social.ToggleBookmark
import com.yet.talk.domain.usecase.social.ToggleLike
import com.yet.talk.domain.usecase.social.ToggleRepost
import com.yet.talk.feature.tab.search.store.SearchStore
import com.yet.talk.feature.tab.search.store.SearchStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchStoreTest {

    private class FakeSearch : SearchRepository {
        val remote = mutableListOf<String>()
        override fun observeResults(query: String): Flow<SearchResults> =
            flowOf(SearchResults(users = listOf(User(UserId(query), "npub1$query"))))

        override suspend fun search(query: String): Result<Unit> {
            remote += query
            return Result.success(Unit)
        }
    }

    private object NoReactions : ReactionRepository {
        override suspend fun setLiked(postId: PostId, liked: Boolean) = Result.success(Unit)
        override suspend fun setReposted(postId: PostId, reposted: Boolean) = Result.success(Unit)
    }

    private object NoBookmarks : BookmarkRepository {
        override suspend fun setBookmarked(postId: PostId, bookmarked: Boolean) = Result.success(Unit)
        override fun bookmarks() = error("unused")
    }

    private val search = FakeSearch()

    private fun TestScope.store(): SearchStore {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return SearchStoreFactory(
            DefaultStoreFactory(), search, ToggleLike(NoReactions), ToggleRepost(NoReactions), ToggleBookmark(NoBookmarks),
        ).create()
    }

    @BeforeTest
    fun setUp() {
        isAssertOnMainThreadEnabled = false
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun localResultsFollowEveryKeystrokeAndRelaysOnlyThePause() = runTest {
        val store = store()
        store.accept(SearchStore.Intent.ChangeQuery("ka"))
        runCurrent()
        assertEquals("ka", store.state.results.users.single().id.value)

        advanceTimeBy(200)
        store.accept(SearchStore.Intent.ChangeQuery("karp"))
        runCurrent()
        assertEquals("karp", store.state.results.users.single().id.value)
        advanceUntilIdle()

        assertEquals(listOf("karp"), search.remote, "the first query was typed over before the pause")
        assertEquals(false, store.state.isSearching)
    }

    @Test
    fun aOneLetterQueryNeverReachesRelays() = runTest {
        val store = store()
        store.accept(SearchStore.Intent.ChangeQuery("k"))
        advanceUntilIdle()

        assertEquals(emptyList<String>(), search.remote)
    }
}
