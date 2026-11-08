package com.yet.talk.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yet.talk.domain.model.error.DomainError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PagingControllerTest {

    /** Numbers 0 until [total], [PAGE] at a time; [failing] makes every load fail. */
    private class Numbers(private val total: Int, private val failing: Boolean = false) : PagingSource<Int, Int>() {
        override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
            if (failing) return LoadResult.Error(DomainError.NotFound)
            val start = params.key ?: 0
            val end = minOf(start + params.loadSize, total)
            return LoadResult.Page((start until end).toList(), prevKey = null, nextKey = end.takeIf { it < total })
        }
    }

    private fun TestScope.controller(source: () -> PagingSource<Int, Int>): PagingController<Int> {
        val pager = Pager(PagingConfig(pageSize = PAGE, initialLoadSize = PAGE, prefetchDistance = 2, enablePlaceholders = false), pagingSourceFactory = source)
        return PagingController(pager.flow, StandardTestDispatcher(testScheduler))
    }

    @Test
    fun startsLoadingThenShowsTheFirstPage() = runTest {
        val controller = controller { Numbers(total = 50) }
        assertTrue(controller.snapshot.value.isFirstLoad)

        advanceUntilIdle()

        val snapshot = controller.snapshot.value
        assertEquals((0 until PAGE).toList(), snapshot.items)
        assertEquals(PagingLoad.Idle, snapshot.refresh)
        assertNull(snapshot.error)
        controller.close()
    }

    @Test
    fun reachingTheEndLoadsTheNextPageUntilComplete() = runTest {
        val controller = controller { Numbers(total = 15) }
        advanceUntilIdle()

        controller.loadAround(controller.snapshot.value.items.lastIndex)
        advanceUntilIdle()

        val snapshot = controller.snapshot.value
        assertEquals((0 until 15).toList(), snapshot.items)
        assertEquals(PagingLoad.Complete, snapshot.append)
        controller.close()
    }

    @Test
    fun anEmptySourceIsEmpty() = runTest {
        val controller = controller { Numbers(total = 0) }
        advanceUntilIdle()

        assertTrue(controller.snapshot.value.isEmpty)
        controller.close()
    }

    @Test
    fun aFailedLoadCarriesItsDomainError() = runTest {
        val controller = controller { Numbers(total = 10, failing = true) }
        advanceUntilIdle()

        val snapshot = controller.snapshot.value
        assertEquals(PagingLoad.Failed, snapshot.refresh)
        assertEquals(DomainError.NotFound, snapshot.error)
        assertTrue(!snapshot.isEmpty && !snapshot.isFirstLoad)
        controller.close()
    }

    private companion object {
        const val PAGE = 10
    }
}
