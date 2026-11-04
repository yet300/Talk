package com.yet.talk.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.native.inMemoryDriver
import com.yet.talk.database.driver.SqlDriverFactory

internal actual fun testDriverFactory(): SqlDriverFactory? = SqlDriverFactory {
    inMemoryDriver(TalkDatabase.Schema.synchronous())
}
