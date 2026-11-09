package com.yet.talk.mcp

import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.mcp.agent.AgentIdentity
import com.yet.talk.mcp.agent.AgentState
import com.yet.talk.mcp.agent.Budget
import com.yet.talk.mcp.agent.PublishGate
import com.yet.talk.mcp.agent.RateLimiter
import com.yet.talk.mcp.agent.RelayProbe
import com.yet.talk.mcp.agent.StateStore
import com.yet.talk.mcp.config.McpConfig
import com.yet.talk.mcp.di.TalkSources
import com.yet.talk.mcp.protocol.McpServer
import com.yet.talk.mcp.talk.TalkChats
import com.yet.talk.mcp.talk.TalkReader
import com.yet.talk.mcp.talk.TalkWriter
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.relay.RelayAuthenticator
import com.yet.talk.nostr.signer.LocalKeySigner
import com.yet.talk.nostr.signer.Signer
import kotlin.time.Clock

/** The agent's relays, from the configuration: it has no NIP-65 list to honour. */
internal class ConfiguredRelays(private val relays: List<String>) : RelaySelection {
    override suspend fun readRelays(): List<String> = relays

    override suspend fun writeRelays(): List<String> = relays
}

/** NIP-42: proves the agent's key to an inbox relay that asks (kind 22242). */
internal class AgentAuthenticator(private val signer: Signer, private val clock: Clock) : RelayAuthenticator {
    override suspend fun authenticate(relayUrl: String, challenge: String): Event =
        signer.sign(EventTemplate(Kinds.CLIENT_AUTH, "", listOf(listOf("relay", relayUrl), listOf("challenge", challenge)), clock.now().epochSeconds))
}

/** Builds the server from its parts; the entry point and the tests differ only in what they pass. */
internal fun createServer(config: McpConfig, sources: TalkSources, store: StateStore, clock: Clock, log: (String) -> Unit): McpServer {
    val now = { clock.now().epochSeconds }
    val signer = LocalKeySigner(config.secretKey)
    val me = config.secretKey.publicKey
    val state = AgentState(store)
    val limiter = RateLimiter(state, mapOf(Budget.Posts to config.posts, Budget.Actions to config.actions))
    val probe = RelayProbe(sources.relays, config.relays)
    val identity = AgentIdentity(me, signer, sources.social, sources.publisher, state, probe, config.agentName, config.model, now)
    val gate = PublishGate(config.publishEnabled, limiter, identity, now)
    val tools = talkTools(
        config,
        me,
        limiter,
        TalkReader(sources, me, now),
        TalkWriter(sources, signer, me, gate, identity, state, probe, now),
        TalkChats(sources.chats, ConfiguredRelays(config.relays), signer, me, gate, now),
        now,
    )
    return McpServer(tools, INSTRUCTIONS, log)
}
