package com.yet.talk.data.paging

/** Fetches one page from relays into the database. */
fun interface PageFetcher {
    /**
     * Fetches events created at or before [until] (null = the newest) and stores them.
     * Returns the oldest event time relays returned, or null when they returned nothing.
     */
    suspend fun fetch(until: Long?): Long?
}
