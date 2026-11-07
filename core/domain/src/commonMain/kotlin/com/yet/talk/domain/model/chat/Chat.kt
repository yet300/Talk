package com.yet.talk.domain.model.chat

import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import kotlin.jvm.JvmInline
import kotlin.time.Instant

/** A private chat (NIP-17): the other members' pubkeys, sorted and comma-joined. */
@JvmInline
value class ChatId(val value: String) {
    val members: List<UserId> get() = value.split(',').filter(String::isNotEmpty).map(::UserId)

    companion object {
        fun of(members: Collection<UserId>) = ChatId(members.map { it.value }.distinct().sorted().joinToString(","))
    }
}

data class Chat(
    val id: ChatId,
    /** The other members; a placeholder user (id and npub) until their profile is known. */
    val members: List<User>,
    val lastMessage: String?,
    val lastAt: Instant,
    val lastState: PublishState?,
    val unread: Int,
    /** From someone you do not follow, and you have not answered or accepted it. */
    val isRequest: Boolean,
)

data class ChatMessage(
    val id: String,
    val chatId: ChatId,
    val author: UserId,
    val isMine: Boolean,
    val content: String,
    val createdAt: Instant,
    val publishState: PublishState,
)
