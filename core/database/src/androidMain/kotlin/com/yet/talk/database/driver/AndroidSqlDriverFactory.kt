package com.yet.talk.database.driver

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.yet.talk.database.TalkDatabase

internal class AndroidSqlDriverFactory(private val context: Context) : SqlDriverFactory {
    override suspend fun create(): SqlDriver =
        AndroidSqliteDriver(TalkDatabase.Schema.synchronous(), context, SqlDriverFactory.DATABASE_NAME)
}

/** The platform graph's [SqlDriverFactory]. */
fun androidSqlDriverFactory(context: Context): SqlDriverFactory = AndroidSqlDriverFactory(context)
