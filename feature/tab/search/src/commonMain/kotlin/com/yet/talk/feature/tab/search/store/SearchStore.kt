package com.yet.talk.feature.tab.search.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.search.SearchResults

internal interface SearchStore : Store<SearchStore.Intent, SearchStore.State, Nothing> {

    data class State(
        val query: String = "",
        val results: SearchResults = SearchResults(),
        val isSearching: Boolean = false,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class ChangeQuery(val query: String) : Intent

        data class Like(val post: Post) : Intent

        data class Repost(val post: Post) : Intent

        data class Bookmark(val post: Post) : Intent

        data object DismissError : Intent
    }

    sealed interface Msg {
        data class QueryChanged(val query: String) : Msg

        data class ResultsChanged(val results: SearchResults) : Msg

        data class Searching(val searching: Boolean) : Msg

        data class Failed(val error: DomainError?) : Msg
    }
}
