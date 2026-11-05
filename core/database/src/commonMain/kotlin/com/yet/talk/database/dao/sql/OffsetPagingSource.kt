package com.yet.talk.database.dao.sql

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.cash.sqldelight.Query
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext

/**
 * Offset paging over SQLDelight queries that works with every driver, including the asynchronous web worker:
 * SQLDelight's own `QueryPagingSource` reads with `executeAsOne`/`executeAsList`, which the async driver
 * rejects. Invalidates itself (so Paging reloads) when the count or the loaded page changes.
 */
internal class OffsetPagingSource<T : Any>(
    private val countQuery: Query<Long>,
    private val context: CoroutineContext,
    private val pageQuery: (limit: Long, offset: Long) -> Query<T>,
) : PagingSource<Int, T>() {

    private val listener = Query.Listener { invalidate() }
    private var pageListenedTo: Query<T>? = null

    init {
        countQuery.addListener(listener)
        registerInvalidatedCallback {
            countQuery.removeListener(listener)
            pageListenedTo?.removeListener(listener)
        }
    }

    override val jumpingSupported: Boolean get() = true

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> = withContext(context) {
        val key = params.key ?: 0
        val limit = when (params) {
            is LoadParams.Prepend -> minOf(key, params.loadSize)
            else -> params.loadSize
        }
        val count = countQuery.awaitAsOne().toInt()
        val offset = when (params) {
            is LoadParams.Prepend -> maxOf(0, key - params.loadSize)
            is LoadParams.Append -> key
            is LoadParams.Refresh -> if (key >= count - params.loadSize) maxOf(0, count - params.loadSize) else key
        }
        val query = pageQuery(limit.toLong(), offset.toLong())
        pageListenedTo?.removeListener(listener)
        query.addListener(listener)
        pageListenedTo = query
        val data = query.awaitAsList()
        val next = offset + data.size
        if (invalid) {
            LoadResult.Invalid()
        } else {
            LoadResult.Page(
                data = data,
                prevKey = offset.takeIf { it > 0 && data.isNotEmpty() },
                nextKey = next.takeIf { data.isNotEmpty() && data.size >= limit && it < count },
                itemsBefore = offset,
                itemsAfter = maxOf(0, count - next),
            )
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? =
        state.anchorPosition?.let { maxOf(0, it - state.config.initialLoadSize / 2) }
}
