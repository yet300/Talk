package com.yet.talk.feature.tab.search.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.SearchRepository
import com.yet.talk.domain.usecase.social.ToggleBookmark
import com.yet.talk.domain.usecase.social.ToggleLike
import com.yet.talk.domain.usecase.social.ToggleRepost
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Inject
internal class SearchStoreFactory(
    private val storeFactory: StoreFactory,
    private val search: SearchRepository,
    private val toggleLike: ToggleLike,
    private val toggleRepost: ToggleRepost,
    private val toggleBookmark: ToggleBookmark,
) {
    fun create(): SearchStore =
        object : SearchStore, Store<SearchStore.Intent, SearchStore.State, Nothing> by storeFactory.create(
            name = "SearchStore",
            initialState = SearchStore.State(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<SearchStore.Intent, Nothing, SearchStore.State, SearchStore.Msg, Nothing>() {
        private var observing: Job? = null
        private var searching: Job? = null

        override fun executeIntent(intent: SearchStore.Intent) {
            when (intent) {
                is SearchStore.Intent.ChangeQuery -> changeQuery(intent.query)
                is SearchStore.Intent.Like -> run { toggleLike(intent.post) }
                is SearchStore.Intent.Repost -> run { toggleRepost(intent.post) }
                is SearchStore.Intent.Bookmark -> run { toggleBookmark(intent.post) }
                SearchStore.Intent.DismissError -> dispatch(SearchStore.Msg.Failed(null))
            }
        }

        /** Local results follow every keystroke; relays are asked once typing pauses. */
        private fun changeQuery(query: String) {
            dispatch(SearchStore.Msg.QueryChanged(query))
            observing?.cancel()
            observing = scope.launch { search.observeResults(query).collect { dispatch(SearchStore.Msg.ResultsChanged(it)) } }
            searching?.cancel()
            if (query.trim().length < SearchRepository.MIN_QUERY_LENGTH) {
                dispatch(SearchStore.Msg.Searching(false))
                return
            }
            searching = scope.launch {
                delay(DEBOUNCE)
                dispatch(SearchStore.Msg.Searching(true))
                val error = search.search(query).exceptionOrNull() as? DomainError
                dispatch(SearchStore.Msg.Searching(false))
                if (error != null) dispatch(SearchStore.Msg.Failed(error))
            }
        }

        private fun run(action: suspend () -> Result<Unit>) {
            scope.launch { action().onFailure { dispatch(SearchStore.Msg.Failed(it as? DomainError)) } }
        }
    }

    private object ReducerImpl : Reducer<SearchStore.State, SearchStore.Msg> {
        override fun SearchStore.State.reduce(msg: SearchStore.Msg): SearchStore.State =
            when (msg) {
                is SearchStore.Msg.QueryChanged -> copy(query = msg.query)
                is SearchStore.Msg.ResultsChanged -> copy(results = msg.results)
                is SearchStore.Msg.Searching -> copy(isSearching = msg.searching)
                is SearchStore.Msg.Failed -> copy(error = msg.error)
            }
    }

    private companion object {
        val DEBOUNCE = 350.milliseconds
    }
}
