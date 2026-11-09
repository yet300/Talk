package com.yet.talk.mcp.agent

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.source.publish.PublishRemoteSource
import com.yet.talk.api.source.social.SocialRemoteSource
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.relay.PublishResult
import com.yet.talk.nostr.signer.Signer

/** The agent's profile on relays, and the guarantee that nothing is published before it says the account is an AI agent. */
internal class AgentIdentity(
    private val pubkey: String,
    private val signer: Signer,
    private val social: SocialRemoteSource,
    private val publisher: PublishRemoteSource,
    private val state: AgentState,
    private val agentName: String?,
    private val model: String?,
    private val now: () -> Long,
) {
    private var disclosed = false

    /** Called before every publication. Creates the profile of a new key, and refuses an account that hides what it is. */
    suspend fun ensureDisclosed() {
        if (disclosed) return
        val latest = latestProfile()
        when {
            latest == null -> publish(template(latest, Disclosure.Edit()))
            !Disclosure.isDisclosed(latest.event.content) -> throw ToolError(
                "This account's profile does not say it is an AI agent, so nothing was published. Call talk_set_profile first.",
            )
            else -> state.profileKnown = true
        }
        disclosed = true
    }

    /** The next kind 0 for [edit], unsigned: the dry run shows this. */
    suspend fun profileTemplate(edit: Disclosure.Edit): EventTemplate = template(latestProfile(), edit)

    suspend fun publish(template: EventTemplate): Pair<Event, PublishResult> {
        val event = signer.sign(template)
        val result = publisher.publish(event)
        if (!result.isAccepted) throw ToolError("No relay accepted the profile, so nothing else was published.")
        state.profileKnown = true
        disclosed = true
        return event to result
    }

    // As in Talk itself: a kind 0 is merged with the latest one, and relays that returned none must not erase it.
    private suspend fun latestProfile(): TalkEvent.Profile? {
        val latest = social.profiles(listOf(pubkey)).filter { it.event.pubkey == pubkey }.maxByOrNull { it.event.createdAt }
        if (latest == null && state.profileKnown) {
            throw ToolError("Relays returned no profile for this account although it has one. Try again later.")
        }
        return latest
    }

    private fun template(latest: TalkEvent.Profile?, edit: Disclosure.Edit): EventTemplate {
        val previous = latest?.let { it.event.content to it.metadata }
        return EventTemplate(
            kind = Kinds.METADATA,
            content = Disclosure.profileContent(previous, edit, agentName, model),
            tags = latest?.event?.tags.orEmpty(),
            createdAt = maxOf(now(), (latest?.event?.createdAt ?: 0) + 1),
        )
    }
}
