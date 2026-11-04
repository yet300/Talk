package com.yet.talk.database.driver

import app.cash.sqldelight.db.SqlDriver

/** Opens the platform driver with the schema created. Bound in each platform's app graph. */
fun interface SqlDriverFactory {
    suspend fun create(): SqlDriver

    companion object {
        const val DATABASE_NAME = "talk.db"
    }
}
