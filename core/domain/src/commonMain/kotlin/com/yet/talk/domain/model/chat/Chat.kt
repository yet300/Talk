package com.yet.talk.domain.model.chat

import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Instant

/** A private chat (NIP-17): the other members' pubkeys, sorted and comma-joined. */
@JvmInline
value class ChatId(val value: String) {
    val members: List<UserId> get() = value.split(',').filter(String::isNotEmpty).map(::UserId)

    val isGroup: Boolean get() = members.size > 1

    companion object {
        fun of(members: Collection<UserId>) = ChatId(members.map { it.value }.distinct().sorted().joinToString(","))
    }
}

data class Chat(
    val id: ChatId,
    /** The other members; a placeholder user (id and npub) until their profile is known. */
    val members: List<User>,
    /** A group's name (NIP-17 subject), if it has one. */
    val name: String?,
    val lastMessage: String?,
    /** The last message is an image ("Photo"). */
    val lastIsImage: Boolean,
    /** You wrote the last message ("You: …"). */
    val lastIsMine: Boolean,
    val lastAt: Instant,
    val lastState: PublishState?,
    val unread: Int,
    /** From someone you do not follow, and you have not answered or accepted it. */
    val isRequest: Boolean,
    val isMuted: Boolean,
    /** Disappearing messages for what you send, or null when off. */
    val disappearing: Duration?,
    /** A one-to-one chat with someone you verified in person. */
    val isVerified: Boolean = false,
)

/**
 * The safety code of you and one contact: the same on both devices, from both public keys. Compared aloud or by
 * scanning [qrPayload] in person.
 */
data class SafetyCode(val groups: List<String>, val qrPayload: String)

data class ChatMessage(
    val id: String,
    val chatId: ChatId,
    val author: UserId,
    val isMine: Boolean,
    /** The text, or empty for an image or an expired message. */
    val content: String,
    val createdAt: Instant,
    val publishState: PublishState,
    val replyTo: Quote? = null,
    val image: ChatImage? = null,
    val reactions: List<Reaction> = emptyList(),
    /** When it disappears, if it does. */
    val expiresAt: Instant? = null,
    /** Its time is up: shown as "This message expired". */
    val isExpired: Boolean = false,
) {
    /** The message replied to, as shown in the bubble; [text] is null when it is gone. */
    data class Quote(val id: String, val author: UserId, val text: String?, val isImage: Boolean)

    data class Reaction(val emoji: String, val count: Int, val isMine: Boolean)
}

/** An encrypted image (NIP-17 kind 15); [ChatRepository.image] downloads and decrypts it. */
data class ChatImage(val messageId: String, val mimeType: String, val size: Long)

/** The choices for disappearing messages (`Chat info`). */
enum class Disappearing(val duration: Duration?) {
    Off(null),
    Hour(Duration.parse("1h")),
    Day(Duration.parse("1d")),
    Week(Duration.parse("7d")),
    ;

    companion object {
        fun of(duration: Duration?) = entries.firstOrNull { it.duration == duration } ?: Off
    }
}
