package com.yet.talk.database.dao

/** Cached GitHub cards, by link (`repo:owner/name` or `user:login`). */
interface GitHubCardDao {
    /** The cached JSON (null: GitHub had none) and when it was fetched; null when not cached. */
    suspend fun get(refKey: String): Pair<String?, Long>?

    suspend fun put(refKey: String, json: String?, fetchedAt: Long)
}
