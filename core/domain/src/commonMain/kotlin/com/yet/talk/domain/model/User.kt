package com.yet.talk.domain.model

data class User(
    val id: UserId,
    val npub: String,
    val name: String? = null,
    val displayName: String? = null,
    val about: String? = null,
    val picture: String? = null,
    val website: String? = null,
    val nip05: String? = null,
)

/** Counts that are expensive on Nostr are null until known. */
data class UserStats(
    val followers: Long? = null,
    val following: Int = 0,
    val posts: Long? = null,
)

data class ProfileEdit(
    val name: String? = null,
    val displayName: String? = null,
    val about: String? = null,
    val picture: String? = null,
    val website: String? = null,
    val nip05: String? = null,
)

data class Account(val userId: UserId, val npub: String)
