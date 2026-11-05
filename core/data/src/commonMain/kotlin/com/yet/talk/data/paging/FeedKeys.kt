package com.yet.talk.data.paging

/** Keys of the feeds' remote paging state in `sync_state`. */
internal object FeedKeys {
    const val LATEST = "latest"
    const val TRENDING = "trending"

    fun following(viewer: String) = "following:$viewer"

    fun posts(author: String) = "posts:$author"

    fun replies(author: String) = "replies:$author"

    fun bookmarks(viewer: String) = "bookmarks:$viewer"

    fun activity(viewer: String) = "activity:$viewer"
}
