package com.yet.talk.mcp.talk

import com.yet.talk.api.schema.PostRef
import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.domain.rule.ContentRules
import com.yet.talk.mcp.agent.AgentIdentity
import com.yet.talk.mcp.agent.AgentState
import com.yet.talk.mcp.agent.Budget
import com.yet.talk.mcp.agent.Disclosure
import com.yet.talk.mcp.agent.PublishGate
import com.yet.talk.mcp.agent.RelayProbe
import com.yet.talk.mcp.agent.ToolError
import com.yet.talk.mcp.di.TalkSources
import com.yet.talk.nostr.encoding.Nip19
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.nip.contactList
import com.yet.talk.nostr.relay.PublishResult
import com.yet.talk.nostr.signer.Signer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Builds every public write from the Talk schema (`TalkEvents`) and sends it through the [PublishGate]. */
internal class TalkWriter(
    private val sources: TalkSources,
    private val signer: Signer,
    private val me: String,
    private val gate: PublishGate,
    private val identity: AgentIdentity,
    private val state: AgentState,
    private val probe: RelayProbe,
    private val now: () -> Long,
) {

    suspend fun post(markdown: String): JsonObject =
        publish(Budget.Posts, TalkEvents.post(content(markdown, isReply = false), emptyList(), null, now()))

    suspend fun reply(postId: String, markdown: String): JsonObject {
        val content = content(markdown, isReply = true)
        val parent = target(postId)
        val root = parent.thread.root?.let { rootId ->
            PostRef(rootId, sources.posts.byIds(listOf(rootId)).firstOrNull()?.event?.pubkey.orEmpty())
        }
        return publish(Budget.Posts, TalkEvents.reply(content, emptyList(), refOf(parent), root, now()))
    }

    suspend fun quote(postId: String, markdown: String): JsonObject {
        val content = content(markdown, isReply = false)
        return publish(Budget.Posts, TalkEvents.post(content, emptyList(), refOf(target(postId)), now()))
    }

    suspend fun like(postId: String): JsonObject {
        val post = target(postId)
        val liked = sources.posts.related(listOf(postId)).any {
            it is TalkEvent.Reaction && it.postId == postId && it.type == TalkEvent.Reaction.Type.Like && it.event.pubkey == me
        }
        if (liked) return unchanged("You already liked this post")
        return publish(Budget.Actions, TalkEvents.like(post.event, now()))
    }

    /** A follow list is replaced whole, so it starts from the newest copy relays have, and never from nothing when one exists. */
    suspend fun follow(pubkey: String): JsonObject {
        if (pubkey == me) throw ToolError("An account cannot follow itself")
        val current = sources.social.lists(me).filterIsInstance<TalkEvent.Contacts>().firstOrNull()
        if (current == null) {
            if (state.contactsKnown) throw ToolError("Relays returned no follow list for this account although it has one. Try again later.")
            probe.requireAllAnswering()
        }
        if (current != null) state.contactsKnown = true
        if (current != null && pubkey in current.pubkeys) return unchanged("You already follow this account")
        val template = contactList(current?.pubkeys.orEmpty() + pubkey, maxOf(now(), (current?.event?.createdAt ?: 0) + 1))
        return gate.run(Budget.Actions, template.toJson()) {
            sendSigned(template).also { state.contactsKnown = true }
        }
    }

    suspend fun setProfile(edit: Disclosure.Edit): JsonObject {
        val template = identity.profileTemplate(edit)
        return gate.run(Budget.Actions, template.toJson(), requireDisclosure = false) {
            val (event, result) = identity.publish(template)
            outcome(event, result)
        }
    }

    private suspend fun publish(budget: Budget, template: EventTemplate) = gate.run(budget, template.toJson()) { sendSigned(template) }

    private suspend fun sendSigned(template: EventTemplate): JsonObject {
        val event = signer.sign(template)
        return outcome(event, sources.publisher.publish(event))
    }

    private fun content(markdown: String, isReply: Boolean): String =
        ContentRules.validatePost(markdown, isReply).getOrElse { throw ToolError(it.message ?: "Invalid content") }

    private suspend fun target(postId: String): TalkEvent.Post =
        sources.posts.byIds(listOf(postId)).firstOrNull() ?: throw ToolError("No Talk post with that id on these relays")

    /** Pins the revision the agent saw, when the post was edited. */
    private suspend fun refOf(post: TalkEvent.Post): PostRef {
        val revision = sources.posts.related(listOf(post.event.id)).filterIsInstance<TalkEvent.Revision>()
            .filter { it.postId == post.event.id && it.event.pubkey == post.event.pubkey }
            .maxWithOrNull(compareBy({ it.number }, { it.event.createdAt }))
        return PostRef(post.event.id, post.event.pubkey, revision?.event?.id)
    }

    private fun unchanged(reason: String) = buildJsonObject {
        put("published", false)
        put("message", reason)
    }

    private fun outcome(event: Event, result: PublishResult) = buildJsonObject {
        put("published", result.isAccepted)
        put("id", event.id)
        if (event.kind == 1) put("note", Nip19.note(event.id))
        put("accepted_by", JsonArray(result.accepted.map(::JsonPrimitive)))
        put("rejected_by", JsonObject(result.rejected.mapValues { JsonPrimitive(it.value) }))
        put("no_answer_from", JsonArray(result.noAnswer.map(::JsonPrimitive)))
    }
}

internal fun EventTemplate.toJson() = buildJsonObject {
    put("kind", kind)
    put("content", content)
    put("tags", JsonArray(tags.map { tag -> JsonArray(tag.map(::JsonPrimitive)) }))
    put("created_at", createdAt)
}
