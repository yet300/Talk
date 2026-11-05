package com.yet.talk.web.paging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.paging.CombinedLoadStates
import androidx.paging.ItemSnapshotList
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

/**
 * Paged items for Compose HTML, like AndroidX `LazyPagingItems`: [peek] renders without loading more,
 * [get] tells Paging the item was reached (the list calls it for the last item when it scrolls into view).
 */
class PagingItems<T : Any> internal constructor(private val flow: Flow<PagingData<T>>) {

    private val presenter = object : PagingDataPresenter<T>(mainContext = Dispatchers.Main) {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) {
            snapshot = snapshot()
        }
    }

    private var snapshot: ItemSnapshotList<T> by mutableStateOf(presenter.snapshot())

    var loadState: CombinedLoadStates by mutableStateOf(InitialLoadStates)
        private set

    val itemCount: Int get() = snapshot.size

    fun peek(index: Int): T? = snapshot[index]

    operator fun get(index: Int): T? {
        presenter[index]
        return snapshot[index]
    }

    fun refresh() = presenter.refresh()

    fun retry() = presenter.retry()

    internal suspend fun collectPagingData() = flow.collectLatest(presenter::collectFrom)

    internal suspend fun collectLoadState() = presenter.loadStateFlow.filterNotNull().collect { loadState = it }

    private companion object {
        val NotLoading = LoadState.NotLoading(endOfPaginationReached = false)
        val InitialLoadStates = CombinedLoadStates(
            refresh = LoadState.Loading,
            prepend = NotLoading,
            append = NotLoading,
            source = LoadStates(refresh = LoadState.Loading, prepend = NotLoading, append = NotLoading),
        )
    }
}

@Composable
fun <T : Any> Flow<PagingData<T>>.collectAsPagingItems(): PagingItems<T> {
    val items = remember(this) { PagingItems(this) }
    LaunchedEffect(items) { items.collectPagingData() }
    LaunchedEffect(items) { items.collectLoadState() }
    return items
}
