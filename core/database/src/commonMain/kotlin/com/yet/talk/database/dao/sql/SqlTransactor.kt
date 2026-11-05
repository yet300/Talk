package com.yet.talk.database.dao.sql

import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.Transactor
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext

/**
 * DAO calls inside [block] join this transaction (SQLDelight nests them). They run on the same
 * [AppDispatchers.io], so their own `withContext(io)` does not dispatch and the transaction stays on
 * one thread, as the Android and native drivers require.
 */
@Inject
internal class SqlTransactor(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : Transactor {
    override suspend fun <R> inTransaction(block: suspend () -> R): R = withContext(dispatchers.io) {
        holder.get().transactionWithResult { block() }
    }
}
