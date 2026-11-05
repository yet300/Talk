package com.yet.talk.feature.tab.search.mapper

import com.yet.talk.domain.repository.SearchRepository
import com.yet.talk.feature.tab.search.SearchComponent
import com.yet.talk.feature.tab.search.store.SearchStore

internal val stateToModel: (SearchStore.State) -> SearchComponent.Model = { state ->
    SearchComponent.Model(
        query = state.query,
        users = state.results.users,
        posts = state.results.posts,
        isSearching = state.isSearching,
        isIdle = state.query.trim().length < SearchRepository.MIN_QUERY_LENGTH,
        error = state.error,
    )
}
