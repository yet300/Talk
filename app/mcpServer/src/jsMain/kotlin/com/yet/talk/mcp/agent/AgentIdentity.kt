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
    private val probe: RelayProbe,
    private val agentName: String?,
    private val model: String?,
    private val now: () -> Long,
) {
    private var disclosed = false

    /** Called before every publication. Creates the profile of a new key, and refuses a key that is someone's account. */
    suspend fun ensureDisclosed() {
        if (disclosed) return
        val latest = agentProfile()
        if (latest == null) publish(template(null, Disclosure.Edit())) else state.profileKnown = true
        disclosed = true
    }

    /** The next kind 0 for [edit], unsigned: the dry run shows this. */
    suspend fun profileTemplate(edit: Disclosure.Edit): EventTemplate = template(agentProfile(), edit)

    suspend fun publish(template: EventTemplate): Pair<Event, PublishResult> {
        val event = signer.sign(template)
        val result = publisher.publish(event)
        if (!result.isAccepted) throw ToolError("No relay accepted the profile, so nothing else was published.")
        state.profileKnown = true
        disclosed = true
        return event to result
    }

    /**
     * The account's latest profile, null for a key with none. As in Talk itself a kind 0 is merged with the latest one,
     * and relays that returned none must not erase it. A key whose profile does not declare an agent is somebody's
     * account: no tool may publish from it or relabel it.
     */
    private suspend fun agentProfile(): TalkEvent.Profile? {
        val latest = social.profiles(listOf(pubkey)).filter { it.event.pubkey == pubkey }.maxByOrNull { it.event.createdAt }
        if (latest == null) {
            if (state.profileKnown) throw ToolError("Relays returned no profile for this account although it has one. Try again later.")
            probe.requireAllAnswering()
            return null
        }
        if (!Disclosure.isDisclosed(latest.event.content)) {
            throw ToolError(
                "Nothing was published. For the operator: this key already belongs to an account that is not a declared AI agent, " +
                    "and this server will not post from it or change its profile. Give this agent its own key.",
            )
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
