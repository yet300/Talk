package com.yet.talk.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.yet.talk.database.driver.SqlDriverFactory

internal actual fun testDriverFactory(): SqlDriverFactory? = SqlDriverFactory {
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { TalkDatabase.Schema.synchronous().create(it) }
}
