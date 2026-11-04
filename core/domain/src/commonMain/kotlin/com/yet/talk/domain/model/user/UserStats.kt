package com.yet.talk.domain.model.user

/** Counts that are expensive on Nostr are null until known. */
data class UserStats(
    val followers: Long? = null,
    val following: Int = 0,
    val posts: Long? = null,
)
