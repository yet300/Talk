package com.yet.talk.mcp.talk

import com.yet.talk.api.schema.ChatRumor
import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.api.source.chat.ChatRemoteSource
import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.domain.repository.ChatRepository
import com.yet.talk.domain.rule.ContentRules
import com.yet.talk.mcp.agent.Budget
import com.yet.talk.mcp.agent.PublishGate
import com.yet.talk.mcp.agent.ToolError
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.nip.Nip59
import com.yet.talk.nostr.nip.Rumor
import com.yet.talk.nostr.signer.Signer
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Private chats (NIP-17), as the apps send and receive them. Nothing is kept: messages are read from the agent's
 * inbox relays and decrypted for each call.
 */
internal class TalkChats(
    private val chats: ChatRemoteSource,
    private val selection: RelaySelection,
    private val signer: Signer,
    private val me: String,
    private val gate: PublishGate,
    private val now: () -> Long,
) {

    suspend fun send(recipients: List<String>, text: String, replyTo: String?, subject: String?): JsonObject {
        val members = recipients.distinct()
        if (members.isEmpty()) throw ToolError("A chat needs at least one recipient")
        if (me in members) throw ToolError("Do not list your own key as a recipient")
        if (members.size > ChatRepository.MAX_GROUP_MEMBERS) throw ToolError("A chat can have at most ${ChatRepository.MAX_GROUP_MEMBERS} other people")
        val content = ContentRules.validatePost(text, isReply = true).getOrElse { throw ToolError(it.message ?: "Invalid message") }
        val inboxes = chats.inboxRelays(members + me)
        val missing = members.filter { it !in inboxes }
        if (missing.isNotEmpty()) throw ToolError("Can't receive chats yet (no inbox relays): ${missing.joinToString()}")
        val template = TalkEvents.chatMessage(members, content, now(), replyTo, subject?.takeIf { members.size > 1 })

        val preview = buildJsonObject {
            put("message", template.toJson())
            put("sent_to_inbox_relays", JsonObject(members.associateWith { member -> JsonArray(inboxes.getValue(member).map(::JsonPrimitive)) }))
        }
        return gate.run(Budget.Actions, preview) {
            val myInbox = inboxes[me] ?: publishInbox()
            val rumor = Rumor.of(me, template)
            val delivered = members.filter { member -> chats.publish(Nip59.wrap(rumor, signer, member, now()), inboxes.getValue(member)) }
            // The agent's own copy, as on any other device.
            chats.publish(Nip59.wrap(rumor, signer, me, now()), myInbox)
            buildJsonObject {
                put("published", delivered.isNotEmpty())
                put("id", rumor.id)
                put("delivered_to", JsonArray(delivered.map(::JsonPrimitive)))
                put("not_delivered_to", JsonArray((members - delivered.toSet()).map(::JsonPrimitive)))
            }
        }
    }

    /** Messages since [since], at most [limit], newest last, optionally only those of the chat with [with]. */
    suspend fun read(with: List<String>?, since: Long?, limit: Int): JsonObject {
        val inbox = chats.inboxRelays(listOf(me))[me] ?: ChatRemoteSource.DEFAULT_INBOX
        val from = since ?: (now() - DEFAULT_LOOKBACK)
        val rumors = wraps(inbox, from - Nip59.MAX_TIME_SHIFT).mapNotNull { Nip59.unwrap(it, signer) }.mapNotNull(ChatRumor::parse)
        val deleted = rumors.filterIsInstance<ChatRumor.Deletion>().flatMap { deletion -> deletion.targets.map { it to deletion.rumor.pubkey } }.toSet()
        val wanted = with?.toSet()
        val messages = rumors.filterIsInstance<ChatRumor.Message>()
            .distinctBy { it.rumor.id }
            .filter { message ->
                val members = message.participants - me
                members.isNotEmpty() && members.size <= ChatRepository.MAX_GROUP_MEMBERS &&
                    (message.rumor.id to message.rumor.pubkey) !in deleted &&
                    message.rumor.createdAt >= from &&
                    message.expiresAt?.let { it <= now() } != true &&
                    (wanted == null || members.toSet() == wanted)
            }
            .sortedBy { it.rumor.createdAt }
            .takeLast(limit)
        return buildJsonObject { put("messages", JsonArray(messages.map(::messageJson))) }
    }

    /**
     * Inbox relays may close a one-shot query with `auth-required`; only an open subscription is sent again after
     * NIP-42 AUTH, and it never says when it is done, so this listens for a fixed time.
     */
    private suspend fun wraps(inbox: List<String>, since: Long): List<Event> {
        val wraps = LinkedHashMap<String, Event>()
        withTimeoutOrNull(LISTEN) { chats.wraps(me, since, inbox).collect { wraps[it.id] = it } }
        return wraps.values.toList()
    }

    private fun messageJson(message: ChatRumor.Message) = buildJsonObject {
        put("id", message.rumor.id)
        put("from", message.rumor.pubkey)
        put("from_me", message.rumor.pubkey == me)
        put("chat_with", JsonArray((message.participants - me).map(::JsonPrimitive)))
        put("created_at", message.rumor.createdAt)
        message.replyTo?.let { put("reply_to", it) }
        message.subject?.let { put("subject", it) }
        when (message) {
            is ChatRumor.Text -> put("text", message.rumor.content)
            is ChatRumor.File -> put("file_type", message.meta.mimeType)
        }
    }

    private suspend fun publishInbox(): List<String> {
        val list = signer.sign(TalkEvents.inboxRelays(ChatRemoteSource.DEFAULT_INBOX, now()))
        chats.publish(list, (selection.writeRelays() + ChatRemoteSource.DEFAULT_INBOX + ChatRemoteSource.INDEXERS).distinct())
        return ChatRemoteSource.DEFAULT_INBOX
    }

    private companion object {
        val LISTEN = 6.seconds
        const val DEFAULT_LOOKBACK = 7 * 24 * 60 * 60L
    }
}
