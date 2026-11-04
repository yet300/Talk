package com.yet.talk.database.model

/** [cursor] is the oldest event time relays returned for the feed; null before the first fetch. */
data class SyncStateRecord(val endReached: Boolean, val cursor: Long?, val syncedAt: Long)
