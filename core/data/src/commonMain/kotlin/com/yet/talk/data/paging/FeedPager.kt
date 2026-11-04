package com.yet.talk.data.paging

import com.yet.talk.data.sync.remote
import com.yet.talk.domain.model.Pager
import com.yet.talk.domain.model.PagingState
import com.yet.talk.domain.model.error.DomainError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

/**
 * KarpathyTalk-style paging over the database. [items] is a window of the newest `limit` rows;
 * [loadMore] fetches the next older page from relays and grows the window by a page. The remote
 * cursor is the oldest event relays returned (a page can hold events the feed does not show).
 * The end is reached when relays return nothing new and the window was not full.
 */
internal class FeedPager<T>(
    private val observe: (limit: Int) -> Flow<List<T>>,
    /** Fetches one page of events created at or before `until` (null = the newest). */
    private val fetch: suspend (until: Long?) -> FetchedPage,
) : Pager<T> {
    private val limit = MutableStateFlow(Pager.PAGE_SIZE)
    private val _state = MutableStateFlow(PagingState())
    private val mutex = Mutex()
    private var shownCount = 0
    private var cursor: Long? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    override val items: Flow<List<T>> = limit.flatMapLatest(observe).onEach { shownCount = it.size }

    override val state: StateFlow<PagingState> = _state.asStateFlow()

    override suspend fun refresh() = load {
        fetch(null).also { page -> if (cursor == null) cursor = page.oldest }
    }

    override suspend fun loadMore() {
        if (_state.value.endReached) return
        load {
            val windowWasFull = shownCount >= limit.value
            // `until` is inclusive on relays: step one second past the oldest event already seen.
            val page = fetch(cursor?.minus(1))
            page.oldest?.let { oldest -> cursor = minOf(cursor ?: oldest, oldest) }
            limit.update { it + Pager.PAGE_SIZE }
            if (page.newItems == 0 && !windowWasFull) _state.update { it.copy(endReached = true) }
            page
        }
    }

    private suspend fun load(block: suspend () -> FetchedPage) {
        if (!mutex.tryLock()) return // a load is already running
        try {
            _state.update { it.copy(isLoading = true, error = null) }
            val error = remote { block() }.exceptionOrNull() as? DomainError
            _state.update { it.copy(isLoading = false, error = error) }
        } finally {
            mutex.unlock()
        }
    }
}
