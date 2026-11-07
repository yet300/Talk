package com.yet.talk.database.model

/** A chat as listed: its members, the last message's encrypted rumor and state, unread count, request flag. */
data class ChatRecord(
    val id: String,
    val members: List<String>,
    val lastAt: Long,
    val isRequest: Boolean,
    val unread: Long,
    val lastRumorEnc: String?,
    val lastState: String?,
)

/** A stored message; [rumorEnc] is the NIP-17 rumor JSON, encrypted at rest. */
data class ChatMessageRecord(
    val id: String,
    val chatId: String,
    val author: String,
    val createdAt: Long,
    val rumorEnc: String,
    val publishState: String,
)
