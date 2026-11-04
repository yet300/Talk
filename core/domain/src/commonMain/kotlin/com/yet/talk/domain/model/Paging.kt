package com.yet.talk.domain.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * A list that grows page by page, KarpathyTalk style ("load more"). [items] always reflects the
 * local database, so it also updates when the user likes, posts or edits.
 */
interface Pager<T> {
    val items: Flow<List<T>>
    val state: StateFlow<PagingState>

    /** Fetches what is newer than the first item. */
    suspend fun refresh()

    /** Fetches the next page, older than the last item. No-op when [PagingState.endReached]. */
    suspend fun loadMore()

    companion object {
        const val PAGE_SIZE = 20
    }
}

data class PagingState(
    val isLoading: Boolean = false,
    val endReached: Boolean = false,
    val error: DomainError? = null,
)
