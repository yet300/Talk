package com.yet.talk.database.model

/** Which list of posts to read. */
sealed interface PostFeed {
    data object Latest : PostFeed

    data object Following : PostFeed

    data class ByAuthor(val author: String, val replies: Boolean) : PostFeed

    data object Bookmarks : PostFeed

    data object Trending : PostFeed
}
