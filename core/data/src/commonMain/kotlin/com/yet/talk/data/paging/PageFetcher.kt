package com.yet.talk.data.paging

/** Fetches one page from relays into the database. */
internal fun interface PageFetcher {
    /** Fetches events created at or before [until] (null = the newest) and stores them. */
    suspend fun fetch(until: Long?): FetchedPage
}

/**
 * [oldest] is the oldest event time relays returned (null when they returned nothing); [size] how
 * many events they returned, and [newItems] how many of those were not stored yet.
 */
internal data class FetchedPage(val oldest: Long?, val size: Int = 0, val newItems: Int = 0) {
    /** A full page of unseen events: older unseen ones may lie between it and what is stored. */
    val mayHaveGap: Boolean get() = size > 0 && newItems == size

    companion object {
        val NOTHING = FetchedPage(oldest = null)
    }
}
