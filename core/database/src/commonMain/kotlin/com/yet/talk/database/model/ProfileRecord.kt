package com.yet.talk.database.model

data class ProfileRecord(
    val pubkey: String,
    val name: String?,
    val displayName: String?,
    val about: String?,
    val picture: String?,
    val website: String?,
    val nip05: String?,
    val updatedAt: Long,
    val banner: String? = null,
)
