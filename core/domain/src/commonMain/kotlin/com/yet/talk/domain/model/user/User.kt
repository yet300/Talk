package com.yet.talk.domain.model.user

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
