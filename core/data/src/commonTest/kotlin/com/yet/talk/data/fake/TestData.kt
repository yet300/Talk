package com.yet.talk.data.fake

import com.app.common.AppDispatchers
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
import com.yet.talk.database.dao.sql.SqlSocialDao
import com.yet.talk.database.dao.sql.SqlTrendingDao
import com.yet.talk.database.driver.SqlDriverFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** The data layer wired by hand over an in-memory database, as Metro would wire it. */
class TestData(factory: SqlDriverFactory, scope: CoroutineScope) {
    val network = FakeNetwork()
    val clock = FixedClock(1_800_000_000)
    val vault = FakeKeyVault()

    private val holder = DatabaseHolder(factory)
    private val dispatchers = AppDispatchers(io = Dispatchers.Default)
    val postDao = SqlPostDao(holder, dispatchers)
    val profileDao = SqlProfileDao(holder, dispatchers)
    val reactionDao = SqlReactionDao(holder, dispatchers)
    val socialDao = SqlSocialDao(holder, dispatchers)
    val trendingDao = SqlTrendingDao(holder, dispatchers)
    val maintenanceDao = SqlMaintenanceDao(holder, dispatchers)

    val session = VaultSession(vault)
    val ingestor = EventIngestor(postDao, profileDao, reactionDao, socialDao)
    val sync = RemoteSync(network.posts, network.social, ingestor)
    val publisher = Publisher(session, network.publisher, ingestor, postDao, clock)

    val accounts = AccountRepositoryImpl(session, maintenanceDao, sync, scope)
    val posts = PostRepositoryImpl(postDao, socialDao, network.posts, sync, ingestor, session, publisher)
    val reactions = ReactionRepositoryImpl(postDao, reactionDao, network.posts, sync, publisher)
    val bookmarks = BookmarkRepositoryImpl(postDao, socialDao, network.posts, sync, ingestor, session, publisher)
    val follows = FollowRepositoryImpl(socialDao, sync, ingestor, session, publisher)
    val trending = TrendingRepositoryImpl(postDao, trendingDao, network.posts, sync, session, publisher)
}
