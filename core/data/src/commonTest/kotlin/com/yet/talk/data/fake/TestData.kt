package com.yet.talk.data.fake

import com.app.common.AppDispatchers
import com.yet.talk.data.paging.FeedPaging
import com.yet.talk.data.repository.AccountRepositoryImpl
import com.yet.talk.data.repository.BookmarkRepositoryImpl
import com.yet.talk.data.repository.FollowRepositoryImpl
import com.yet.talk.data.repository.PostRepositoryImpl
import com.yet.talk.data.repository.ReactionRepositoryImpl
import com.yet.talk.data.repository.TrendingRepositoryImpl
import com.yet.talk.data.session.VaultSession
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.sql.SqlMaintenanceDao
import com.yet.talk.database.dao.sql.SqlPostDao
import com.yet.talk.database.dao.sql.SqlProfileDao
import com.yet.talk.database.dao.sql.SqlReactionDao
import com.yet.talk.database.dao.sql.SqlActivityDao
import com.yet.talk.database.dao.sql.SqlBookmarkDao
import com.yet.talk.database.dao.sql.SqlFollowDao
import com.yet.talk.database.dao.sql.SqlRelayListDao
import com.yet.talk.database.dao.sql.SqlSyncDao
import com.yet.talk.database.dao.sql.SqlTrendingDao
import com.yet.talk.database.driver.SqlDriverFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher

/** The data layer wired by hand over an in-memory database, as Metro would wire it. */
class TestData(factory: SqlDriverFactory, scope: CoroutineScope, io: CoroutineDispatcher) {
    val network = FakeNetwork()
    val clock = FixedClock(1_800_000_000)
    val vault = FakeKeyVault()

    private val holder = DatabaseHolder(factory)
    private val dispatchers = AppDispatchers(io = io)
    val postDao = SqlPostDao(holder, dispatchers)
    val profileDao = SqlProfileDao(holder, dispatchers)
    val reactionDao = SqlReactionDao(holder, dispatchers)
    val followDao = SqlFollowDao(holder, dispatchers)
    val bookmarkDao = SqlBookmarkDao(holder, dispatchers)
    val relayListDao = SqlRelayListDao(holder, dispatchers)
    val activityDao = SqlActivityDao(holder, dispatchers)
    val syncDao = SqlSyncDao(holder, dispatchers)
    val trendingDao = SqlTrendingDao(holder, dispatchers)
    val maintenanceDao = SqlMaintenanceDao(holder, dispatchers)

    val session = VaultSession(vault)
    val ingestor = EventIngestor(postDao, profileDao, reactionDao, followDao, bookmarkDao, relayListDao)
    val sync = RemoteSync(network.posts, network.social, ingestor)
    val publisher = Publisher(session, network.publisher, ingestor, postDao, clock)
    val paging = FeedPaging(syncDao, clock)

    val accounts = AccountRepositoryImpl(session, maintenanceDao, sync, scope)
    val posts = PostRepositoryImpl(postDao, followDao, network.posts, sync, ingestor, session, publisher, paging)
    val reactions = ReactionRepositoryImpl(postDao, reactionDao, network.posts, sync, publisher)
    val bookmarks = BookmarkRepositoryImpl(postDao, bookmarkDao, network.posts, sync, ingestor, session, publisher, paging)
    val follows = FollowRepositoryImpl(followDao, sync, ingestor, session, publisher)
    val trending = TrendingRepositoryImpl(postDao, trendingDao, network.posts, sync, session, publisher, paging)
}
