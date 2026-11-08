package com.yet.talk.paging

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * A paged list for SwiftUI, like AndroidX `LazyPagingItems` and the web's `PagingItems`: collects [flow] with a
 * [PagingDataPresenter] and publishes what is loaded as [snapshot]. The list calls [loadAround] for the rows it shows
 * (the last one loads the next page), [refresh] on pull to refresh and [retry] after an error. Main thread only.
 *
 * Swift: `PagingController(flow: component.posts)`, observed with `StateValue(controller.snapshot)`, and [close] once
 * the list goes away.
 */
class PagingController<T : Any> internal constructor(
    flow: Flow<PagingData<T>>,
    mainContext: CoroutineContext,
) {
    constructor(flow: Flow<PagingData<T>>) : this(flow, Dispatchers.Main)

    private val scope = CoroutineScope(SupervisorJob() + mainContext)

    private val presenter = object : PagingDataPresenter<T>(mainContext = mainContext) {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) {
            publish()
        }
    }

    private var loadStates: CombinedLoadStates? = null

    private val state = MutableValue(PagingSnapshot<T>(items = emptyList(), refresh = PagingLoad.Loading, append = PagingLoad.Idle, error = null))

    /** The loaded rows and where loading stands. */
    val snapshot: Value<PagingSnapshot<T>> = state

    init {
        scope.launch { flow.collectLatest(presenter::collectFrom) }
        scope.launch {
            presenter.loadStateFlow.filterNotNull().collect {
                loadStates = it
                publish()
            }
        }
    }

    /** The row at [index] of [PagingSnapshot.items] is on screen: Paging fetches the next page near the end. */
    fun loadAround(index: Int) {
        val position = presenter.snapshot().placeholdersBefore + index
        if (position in 0 until presenter.size) presenter[position]
    }

    fun refresh() = presenter.refresh()

    fun retry() = presenter.retry()

    /** Stops collecting; the controller is not used again. */
    fun close() = scope.cancel()

    private fun publish() {
        val states = loadStates
        val refresh = states?.refresh
        val append = states?.append
        val error = (refresh as? LoadState.Error ?: append as? LoadState.Error)?.error
        state.value = PagingSnapshot(
            items = presenter.snapshot().items,
            refresh = refresh?.toLoad() ?: PagingLoad.Loading,
            append = append?.toLoad() ?: PagingLoad.Idle,
            error = error?.let { it as? DomainError ?: DomainError.Network(it) },
        )
    }

    private fun LoadState.toLoad(): PagingLoad = when (this) {
        is LoadState.Loading -> PagingLoad.Loading
        is LoadState.Error -> PagingLoad.Failed
        is LoadState.NotLoading -> if (endOfPaginationReached) PagingLoad.Complete else PagingLoad.Idle
    }
}

/** Where a load stands: [Complete] once the end has been reached. */
enum class PagingLoad { Idle, Loading, Complete, Failed }

/** What a paged list shows: [items] in order, the first load ([refresh]), the next page ([append]) and an error. */
data class PagingSnapshot<T : Any>(
    val items: List<T>,
    val refresh: PagingLoad,
    val append: PagingLoad,
    val error: DomainError?,
) {
    /** Nothing loaded yet: the skeleton rows show. */
    val isFirstLoad: Boolean get() = items.isEmpty() && refresh == PagingLoad.Loading

    /** Loaded, and nothing there: the empty state shows. */
    val isEmpty: Boolean get() = items.isEmpty() && refresh != PagingLoad.Loading && refresh != PagingLoad.Failed
}
