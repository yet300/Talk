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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * SQLite WASM in a module worker, persisted in OPFS (see `resources/sqlite/talk-sqlite.worker.mjs`).
 * The web app serves the worker at [workerPath], relative to the page.
 *
 * The database survives reloads, so the schema is created once and migrated by `PRAGMA user_version`,
 * the way the Android and native drivers do it.
 *
 * A worker whose script does not load (a missing or half-written file, a page served while the build
 * rewrites it) fires a bare `error` event and every query on it fails, so the first query doubles as a
 * start-up check: on failure the worker is stopped and started again, [attempts] times with a growing pause.
 */
internal class WebSqlDriverFactory(
    private val workerPath: String = "sqlite/talk-sqlite.worker.mjs",
    private val attempts: Int = 3,
) : SqlDriverFactory {

    override suspend fun create(): SqlDriver {
        val (driver, version) = started()
        val schema = TalkDatabase.Schema
        when (version) {
            0L -> schema.awaitCreate(driver)
            in 1 until schema.version -> schema.awaitMigrate(driver, version, schema.version)
            else -> return driver
        }
        driver.execute(null, "PRAGMA user_version = ${schema.version}", 0).await()
        return driver
    }

    /** A worker that answered its first query, with the schema version it read. */
    private suspend fun started(): Pair<SqlDriver, Long> {
        val url = URL(workerPath, document.baseURI).href
        var attempt = 1
        while (true) {
            val driver = WebWorkerDriver(Worker(url, WorkerOptions(type = WorkerType.MODULE)))
            try {
                return driver to driver.userVersion()
            } catch (cancelled: CancellationException) {
                driver.close()
                throw cancelled
            } catch (failure: Throwable) {
                driver.close()
                if (attempt >= attempts) throw IllegalStateException("The SQLite worker at $url did not start", failure)
                console.warn("Talk: the SQLite worker did not start (attempt $attempt of $attempts), retrying", failure)
                delay(RETRY_PAUSE * attempt)
                attempt++
            }
        }
    }

    private suspend fun SqlDriver.userVersion(): Long =
        executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor -> QueryResult.AsyncValue { if (cursor.next().await()) cursor.getLong(0) ?: 0L else 0L } },
            parameters = 0,
        ).await()

    private companion object {
        val RETRY_PAUSE = 500.milliseconds
    }
}

/** The platform graph's [SqlDriverFactory]. */
fun webSqlDriverFactory(): SqlDriverFactory = WebSqlDriverFactory()
