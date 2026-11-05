package com.yet.talk.domain.repository

import com.yet.talk.domain.model.search.SearchResults
import kotlinx.coroutines.flow.Flow

/** Search over the local cache, so it works offline; [search] asks NIP-50 relays to fill it. */
interface SearchRepository {
    fun observeResults(query: String): Flow<SearchResults>

    suspend fun search(query: String): Result<Unit>

    companion object {
        /** Shorter queries match too much to be useful. */
        const val MIN_QUERY_LENGTH = 2
    }
}
