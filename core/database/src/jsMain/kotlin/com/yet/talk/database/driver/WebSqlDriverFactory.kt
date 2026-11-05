package com.yet.talk.database.driver

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.async.coroutines.awaitMigrate
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.yet.talk.database.TalkDatabase
import kotlinx.browser.document
import org.w3c.dom.MODULE
import org.w3c.dom.Worker
import org.w3c.dom.WorkerOptions
import org.w3c.dom.WorkerType
import org.w3c.dom.url.URL

/**
 * SQLite WASM in a module worker, persisted in OPFS (see `resources/sqlite/talk-sqlite.worker.mjs`).
 * The web app serves the worker at [workerPath], relative to the page.
 *
 * The database survives reloads, so the schema is created once and migrated by `PRAGMA user_version`,
 * the way the Android and native drivers do it.
 */
internal class WebSqlDriverFactory(private val workerPath: String = "sqlite/talk-sqlite.worker.mjs") : SqlDriverFactory {

    override suspend fun create(): SqlDriver {
        val url = URL(workerPath, document.baseURI).href
        val driver = WebWorkerDriver(Worker(url, WorkerOptions(type = WorkerType.MODULE)))
        val schema = TalkDatabase.Schema
        when (val version = driver.userVersion()) {
            0L -> schema.awaitCreate(driver)
            in 1 until schema.version -> schema.awaitMigrate(driver, version, schema.version)
            else -> return driver
        }
        driver.execute(null, "PRAGMA user_version = ${schema.version}", 0).await()
        return driver
    }

    private suspend fun SqlDriver.userVersion(): Long =
        executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor -> QueryResult.AsyncValue { if (cursor.next().await()) cursor.getLong(0) ?: 0L else 0L } },
            parameters = 0,
        ).await()
}

/** The platform graph's [SqlDriverFactory]. */
fun webSqlDriverFactory(): SqlDriverFactory = WebSqlDriverFactory()
