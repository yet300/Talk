package com.yet.talk.data.paging

/** One fetched page: how many items were new locally, and the oldest event time relays returned. */
internal data class FetchedPage(val newItems: Int, val oldest: Long?)
