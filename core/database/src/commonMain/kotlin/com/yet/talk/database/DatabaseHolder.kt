package com.yet.talk.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.yet.talk.database.driver.SqlDriverFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/** Opens the database on first use: on the web, creating the schema is asynchronous. */
@SingleIn(AppScope::class)
@Inject
internal class DatabaseHolder(private val driverFactory: SqlDriverFactory) {
    private val mutex = Mutex()

    @Volatile
    private var database: TalkDatabase? = null

    // Lock-free once open: every DAO call goes through here, and a suspension inside a transaction
    // could resume on another thread, which the Android and native drivers do not allow.
    suspend fun get(): TalkDatabase = database ?: mutex.withLock {
        database ?: TalkDatabase(driverFactory.create()).also { database = it }
    }

    internal fun <T : Any> observeList(context: CoroutineContext, query: TalkDatabase.() -> Query<T>): Flow<List<T>> =
        flow { emitAll(get().query().asFlow().mapToList(context)) }

    internal fun <T : Any> observeOneOrNull(context: CoroutineContext, query: TalkDatabase.() -> Query<T>): Flow<T?> =
        flow { emitAll(get().query().asFlow().mapToOneOrNull(context)) }

    internal fun <T : Any> observeOne(context: CoroutineContext, query: TalkDatabase.() -> Query<T>): Flow<T> =
        flow { emitAll(get().query().asFlow().mapToOne(context)) }
}
