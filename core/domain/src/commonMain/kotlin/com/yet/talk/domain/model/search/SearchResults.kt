package com.yet.talk.domain.model.search

import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.user.User

/** People and posts matching a query, from the local cache (and what relays found for it). */
data class SearchResults(
    val users: List<User> = emptyList(),
    val posts: List<Post> = emptyList(),
)
