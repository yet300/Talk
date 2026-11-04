package com.yet.talk.database.dao.sql

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.yet.talk.database.TalkDatabase

/**
 * Replaces a replaceable list (NIP-01 kinds 10000+ and 3) if [createdAt] is newer than the stored
 * version. Runs inside a transaction so the version check and the write are atomic.
 */
internal suspend fun TalkDatabase.replaceList(pubkey: String, kind: Int, createdAt: Long, write: suspend TalkDatabase.() -> Unit): Boolean =
    transactionWithResult {
        val stored = replaceableVersionQueries.selectVersion(pubkey, kind.toLong()).awaitAsOneOrNull()
        if (stored != null && stored >= createdAt) return@transactionWithResult false
        write()
        replaceableVersionQueries.setVersion(pubkey, kind.toLong(), createdAt)
        true
    }

internal suspend fun TalkDatabase.listVersion(pubkey: String, kind: Int): Long? =
    replaceableVersionQueries.selectVersion(pubkey, kind.toLong()).awaitAsOneOrNull()

// Mirror the Nostr kinds; the database does not depend on core:nostr.
internal const val KIND_CONTACTS = 3
internal const val KIND_RELAY_LIST = 10002
internal const val KIND_BOOKMARKS = 10003
