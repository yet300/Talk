package com.yet.talk.database.di

import com.yet.talk.database.dao.ActivityDao
import com.yet.talk.database.dao.BookmarkDao
import com.yet.talk.database.dao.FollowDao
import com.yet.talk.database.dao.GitHubCardDao
import com.yet.talk.database.dao.VideoDao
import com.yet.talk.database.dao.ReportDao
import com.yet.talk.database.dao.ChatDao
import com.yet.talk.database.dao.OutboxDao
import com.yet.talk.database.dao.sql.SqlOutboxDao
import com.yet.talk.database.dao.MaintenanceDao
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.dao.ProfileDao
import com.yet.talk.database.dao.ReactionDao
import com.yet.talk.database.dao.RelayListDao
import com.yet.talk.database.dao.SyncDao
import com.yet.talk.database.dao.TrendingDao
import com.yet.talk.database.dao.Transactor
import com.yet.talk.database.dao.sql.SqlActivityDao
import com.yet.talk.database.dao.sql.SqlBookmarkDao
import com.yet.talk.database.dao.sql.SqlFollowDao
import com.yet.talk.database.dao.sql.SqlGitHubCardDao
import com.yet.talk.database.dao.sql.SqlVideoDao
import com.yet.talk.database.dao.sql.SqlReportDao
import com.yet.talk.database.dao.sql.SqlChatDao
import com.yet.talk.database.dao.sql.SqlMaintenanceDao
import com.yet.talk.database.dao.sql.SqlPostDao
import com.yet.talk.database.dao.sql.SqlProfileDao
import com.yet.talk.database.dao.sql.SqlReactionDao
import com.yet.talk.database.dao.sql.SqlRelayListDao
import com.yet.talk.database.dao.sql.SqlSyncDao
import com.yet.talk.database.dao.sql.SqlTransactor
import com.yet.talk.database.dao.sql.SqlTrendingDao
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

/**
 * DAO bindings contributed to the app-wide [AppScope] graph. The SQLDelight implementations are
 * `internal`; only the DAO interfaces leave this module. The platform graph provides the
 * [com.yet.talk.database.driver.SqlDriverFactory].
 */
@ContributesTo(AppScope::class)
@BindingContainer
abstract class DatabaseBindings {

    @Binds
    internal abstract val SqlPostDao.bindPostDao: PostDao

    @Binds
    internal abstract val SqlProfileDao.bindProfileDao: ProfileDao

    @Binds
    internal abstract val SqlReactionDao.bindReactionDao: ReactionDao

    @Binds
    internal abstract val SqlFollowDao.bindFollowDao: FollowDao

    @Binds
    internal abstract val SqlBookmarkDao.bindBookmarkDao: BookmarkDao

    @Binds
    internal abstract val SqlRelayListDao.bindRelayListDao: RelayListDao

    @Binds
    internal abstract val SqlActivityDao.bindActivityDao: ActivityDao

    @Binds
    internal abstract val SqlTrendingDao.bindTrendingDao: TrendingDao

    @Binds
    internal abstract val SqlSyncDao.bindSyncDao: SyncDao

    @Binds
    internal abstract val SqlGitHubCardDao.bindGitHubCardDao: GitHubCardDao

    @Binds
    internal abstract val SqlVideoDao.bindVideoDao: VideoDao

    @Binds
    internal abstract val SqlReportDao.bindReportDao: ReportDao

    @Binds
    internal abstract val SqlChatDao.bindChatDao: ChatDao

    @Binds
    internal abstract val SqlOutboxDao.bindOutboxDao: OutboxDao

    @Binds
    internal abstract val SqlMaintenanceDao.bindMaintenanceDao: MaintenanceDao

    @Binds
    internal abstract val SqlTransactor.bindTransactor: Transactor
}
