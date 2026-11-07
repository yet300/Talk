package com.yet.talk.database.model

/** A chat as listed: its members, the last message's encrypted rumor and state, unread count, request flag. */
data class ChatRecord(
    val id: String,
    val members: List<String>,
    val lastAt: Long,
    /** A group's name, encrypted at rest. */
    val nameEnc: String?,
    val muted: Boolean,
    val expireSeconds: Long?,
    val isRequest: Boolean,
    val unread: Long,
    val lastRumorEnc: String?,
    val lastState: String?,
)

/** A stored message; [rumorEnc] is the NIP-17 rumor JSON, encrypted at rest (empty once removed). */
data class ChatMessageRecord(
    val id: String,
    val chatId: String,
    val author: String,
    val createdAt: Long,
    val rumorEnc: String,
    val publishState: String,
    val kind: Long = 14,
    val replyTo: String? = null,
    val expiresAt: Long? = null,
    val removed: Long = MessageRemoval.SHOWN,
)

data class ChatReactionRecord(val messageId: String, val author: String, val emoji: String)

/** [ChatMessageRecord.removed] values. */
object MessageRemoval {
    const val SHOWN = 0L
    const val FOR_ME = 1L
    const val BY_AUTHOR = 2L
    const val EXPIRED = 3L
}
