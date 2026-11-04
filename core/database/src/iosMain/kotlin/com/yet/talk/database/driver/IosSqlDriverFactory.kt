package com.yet.talk.database.driver

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.yet.talk.database.TalkDatabase

class IosSqlDriverFactory : SqlDriverFactory {
    override suspend fun create(): SqlDriver =
        NativeSqliteDriver(TalkDatabase.Schema.synchronous(), SqlDriverFactory.DATABASE_NAME)
}
