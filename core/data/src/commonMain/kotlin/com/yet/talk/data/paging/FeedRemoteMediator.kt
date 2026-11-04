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
            val append = loadType == LoadType.APPEND
            if (append && stored?.endReached == true) return@remote true
            // `until` is inclusive on relays: step one second past the oldest event already seen.
            val oldest = fetcher.fetch(until = if (append) stored?.cursor?.minus(1) else null)
            val endReached = if (append) oldest == null else stored?.endReached == true
            val cursor = listOfNotNull(stored?.cursor, oldest).minOrNull()
            syncDao.setState(feedKey, SyncStateRecord(endReached, cursor, clock.now().epochSeconds))
            endReached
        }.fold(
            onSuccess = { MediatorResult.Success(endOfPaginationReached = it) },
            onFailure = { MediatorResult.Error(it) },
        )
    }
}
