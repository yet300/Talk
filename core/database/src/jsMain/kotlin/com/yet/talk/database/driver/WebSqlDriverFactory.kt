package com.yet.talk.database.driver

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.yet.talk.database.TalkDatabase
import org.w3c.dom.Worker

/**
 * The web app creates the sql.js worker, because only its bundler can resolve the worker URL:
 * `new Worker(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url))`.
 * The database lives in memory, so the web cache starts empty on every page load.
 */
class WebSqlDriverFactory(private val worker: Worker) : SqlDriverFactory {
    override suspend fun create(): SqlDriver =
        WebWorkerDriver(worker).also { TalkDatabase.Schema.awaitCreate(it) }
}
