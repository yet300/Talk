package com.yet.talk.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.yet.talk.database.dao.SyncDao
import com.yet.talk.domain.rule.FeedRules
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

/** Builds KarpathyTalk-style paged feeds: 20 rows a page from the database, older pages from relays. */
@Inject
class FeedPaging(private val syncDao: SyncDao, private val clock: Clock) {

    /**
     * [feedKey] identifies the feed's remote cursor (include the viewer for per-user feeds). [pages]
     * opens the database lazily, which is asynchronous on the web.
     */
    @OptIn(ExperimentalPagingApi::class)
    fun <T : Any> feed(
        feedKey: String,
        pages: suspend () -> () -> PagingSource<Int, T>,
        fetcher: PageFetcher,
    ): Flow<PagingData<T>> = flow {
        val pagingSourceFactory = pages()
        emitAll(
            Pager(
                config = PagingConfig(
                    pageSize = FeedRules.PAGE_SIZE,
                    initialLoadSize = FeedRules.PAGE_SIZE,
                    enablePlaceholders = false,
                ),
                remoteMediator = FeedRemoteMediator(feedKey, syncDao, clock, fetcher),
                pagingSourceFactory = pagingSourceFactory,
            ).flow,
        )
    }
}
