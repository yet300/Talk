package com.yet.talk.mcp.agent

import com.yet.talk.nostr.message.Filter
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.relay.RelayClient
import kotlin.time.Duration.Companion.seconds

/**
 * Tells "this key has nothing here" from "nobody answered". A relay query reports only the events that arrived, so
 * an outage looks like an empty account. Before something replaceable is created from nothing, every configured relay
 * must prove it is serving by returning any profile at all.
 */
internal class RelayProbe(private val relays: RelayClient, private val urls: List<String>) {

    suspend fun requireAllAnswering() {
        val silent = urls.filter { url -> relays.query(ANY_PROFILE, listOf(url), TIMEOUT).isEmpty() }
        if (silent.isNotEmpty()) {
            throw ToolError(
                "Nothing was published: these relays did not answer, so an existing profile or follow list could be overwritten: " +
                    silent.joinToString() + ". Try again later. If a relay is gone, its operator should remove it from TALK_MCP_RELAYS.",
            )
        }
    }

    private companion object {
        val ANY_PROFILE = listOf(Filter(kinds = listOf(Kinds.METADATA), limit = 1))
        val TIMEOUT = 5.seconds
    }
}
