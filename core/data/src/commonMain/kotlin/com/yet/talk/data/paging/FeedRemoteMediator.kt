package com.yet.talk.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.SyncDao
import com.yet.talk.database.model.SyncStateRecord
import kotlin.time.Clock

/**
 * Relays → database for one feed; the UI pages over the database. The remote cursor is the oldest
 * event relays returned (a page can hold events the feed does not show, such as replies in Latest),
 * kept in `sync_state` so paging resumes after a restart. Relays have no more when an older page
 * comes back empty.
 *
 * A refresh after a long pause can return a full page of unseen events with more unseen ones
 * behind it. It keeps fetching older pages until it reaches stored events, up to [MAX_GAP_PAGES];
 * past that, the cursor restarts below the newest pages so scrolling fills the rest of the gap.
 */
@OptIn(ExperimentalPagingApi::class)
internal class FeedRemoteMediator<T : Any>(
    private val feedKey: String,
    private val syncDao: SyncDao,
    private val clock: Clock,
    private val fetcher: PageFetcher,
) : RemoteMediator<Int, T>() {

    override suspend fun initialize(): InitializeAction = InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(loadType: LoadType, state: PagingState<Int, T>): MediatorResult {
        // New posts arrive through refresh; there is nothing newer to prepend.
        if (loadType == LoadType.PREPEND) return MediatorResult.Success(endOfPaginationReached = true)
        return remote {
            val stored = syncDao.state(feedKey)
            val (cursor, endReached) = if (loadType == LoadType.APPEND) {
                if (stored?.endReached == true) return@remote true
                // `until` is inclusive on relays: step one second past the oldest event already seen.
                val page = fetcher.fetch(until = stored?.cursor?.minus(1))
                oldestOf(stored?.cursor, page.oldest) to (page.oldest == null)
            } else {
                refresh(stored)
            }
            syncDao.setState(feedKey, SyncStateRecord(endReached, cursor, clock.now().epochSeconds))
            endReached
        }.fold(
            onSuccess = { MediatorResult.Success(endOfPaginationReached = it) },
            onFailure = { MediatorResult.Error(it) },
        )
    }

    private suspend fun refresh(stored: SyncStateRecord?): Pair<Long?, Boolean> {
        var page = fetcher.fetch(until = null)
        if (stored?.cursor == null) return page.oldest to false
        var pages = 1
        while (page.mayHaveGap && pages < MAX_GAP_PAGES) {
            page = fetcher.fetch(until = page.oldest!! - 1)
            pages++
        }
        return if (page.mayHaveGap) page.oldest to false else oldestOf(stored.cursor, page.oldest) to stored.endReached
    }

    private fun oldestOf(a: Long?, b: Long?): Long? = listOfNotNull(a, b).minOrNull()

    private companion object {
        const val MAX_GAP_PAGES = 5
    }
}
