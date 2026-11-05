package com.yet.talk.database.dao

/**
 * Runs several DAO writes as one transaction: one commit, and one change notification to paging
 * sources and flows instead of one per write.
 */
interface Transactor {
    suspend fun <R> inTransaction(block: suspend () -> R): R
}
