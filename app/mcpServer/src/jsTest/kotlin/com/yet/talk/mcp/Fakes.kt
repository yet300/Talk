package com.yet.talk.mcp

import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.mcp.agent.StateStore
import com.yet.talk.mcp.config.McpConfig
import com.yet.talk.mcp.config.RateLimit
import com.yet.talk.mcp.di.TalkSources
import com.yet.talk.mcp.protocol.McpServer
import com.yet.talk.nostr.di.NostrBindings
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.message.Filter
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.relay.PublishResult
import com.yet.talk.nostr.relay.RelayClient
import com.yet.talk.nostr.signer.LocalKeySigner
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** In-memory relays: nothing here can open a socket, so tests never reach a public relay. */
internal class FakeRelays : RelayClient {
    /** Every real relay holds some profile; this one is what proves a relay is answering. */
    val stored = mutableListOf(Event("0".repeat(64), "1".repeat(64), 1, Kinds.METADATA, emptyList(), "{}", ""))

    /** Relays that are down: asked alone they return nothing, as an unreachable relay does. */
    var silent: Set<String> = emptySet()
    val published = mutableListOf<Pair<Event, Set<String>>>()
    var acceptEvents = true

    override suspend fun query(filters: List<Filter>, relays: Collection<String>, timeout: Duration): List<Event> =
        if (relays.all { it in silent }) emptyList() else stored.filter { event -> filters.any { it.matches(event) } }.sortedByDescending { it.createdAt }

    override fun subscribe(filters: List<Filter>, relays: Collection<String>): Flow<Event> = flow {
        stored.filter { event -> filters.any { it.matches(event) } }.forEach { emit(it) }
        awaitCancellation()
    }

    override suspend fun publish(event: Event, relays: Collection<String>, timeout: Duration): PublishResult {
        published += event to relays.toSet()
        if (acceptEvents) stored += event
        return if (acceptEvents) PublishResult(relays.toSet(), emptyMap(), emptySet()) else PublishResult(emptySet(), relays.associateWith { "blocked: test" }, emptySet())
    }

    override suspend fun count(filters: List<Filter>, relays: Collection<String>, timeout: Duration): Long? = null

    private fun Filter.matches(event: Event): Boolean =
        (ids == null || event.id in ids!!) &&
            (authors == null || event.pubkey in authors!!) &&
            (kinds == null || event.kind in kinds!!) &&
            (since == null || event.createdAt >= since!!) &&
            (until == null || event.createdAt <= until!!) &&
            (search == null || search!! in event.content) &&
            tags.all { (name, values) -> event.tags.any { it.size >= 2 && it[0] == name.toString() && it[1] in values } }
}

/** The real `core:api` sources over [FakeRelays]. */
@DependencyGraph(AppScope::class, excludes = [NostrBindings::class])
internal interface TestGraph : TalkSources {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides relays: RelayClient, @Provides selection: RelaySelection): TestGraph
    }
}

internal class MemoryStore(var text: String? = null) : StateStore {
    override fun load(): String? = text

    override fun save(text: String) {
        this.text = text
    }
}

internal class TestClock(var seconds: Long = 1_800_000_000) : Clock {
    override fun now(): Instant = Instant.fromEpochSeconds(seconds)
}

internal val TEST_RELAYS = listOf("wss://fake.test")

internal fun testConfig(
    key: SecretKey,
    publishEnabled: Boolean,
    model: String? = null,
    posts: RateLimit = RateLimit(4, 300),
    actions: RateLimit = RateLimit(60, 5),
    urls: List<String> = TEST_RELAYS,
) = McpConfig(key, publishEnabled, urls, "Scout", model, posts, actions, "/nowhere")

/** One agent connected to [relays]. */
internal class Agent(
    val relays: FakeRelays = FakeRelays(),
    val clock: TestClock = TestClock(),
    val key: SecretKey = SecretKey.generate(),
    publishEnabled: Boolean = true,
    model: String? = null,
    val store: StateStore = MemoryStore(),
    posts: RateLimit = RateLimit(4, 300),
    actions: RateLimit = RateLimit(60, 5),
    urls: List<String> = TEST_RELAYS,
) {
    val pubkey: String = key.publicKey
    val logs = mutableListOf<String>()
    val server: McpServer = createServer(
        testConfig(key, publishEnabled, model, posts, actions, urls),
        createGraphFactory<TestGraph.Factory>().create(relays, ConfiguredRelays(urls)),
        store,
        clock,
        logs::add,
    )
    private var nextId = 1

    suspend fun rpc(method: String, params: JsonObject = JsonObject(emptyMap())): JsonObject {
        val answer = server.handle(
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", nextId++)
                put("method", method)
                put("params", params)
            }.toString(),
        )
        return Json.parseToJsonElement(answer!!).jsonObject
    }

    /** The tool's result text, parsed when it is JSON, and whether it failed. */
    suspend fun call(tool: String, vararg arguments: Pair<String, JsonElement>): ToolOutcome {
        val result = rpc(
            "tools/call",
            buildJsonObject {
                put("name", tool)
                put("arguments", JsonObject(arguments.toMap()))
            },
        ).getValue("result").jsonObject
        val text = result.getValue("content").jsonArray[0].jsonObject.getValue("text").jsonPrimitive.content
        return ToolOutcome(result.getValue("isError").jsonPrimitive.content == "true", text)
    }

    fun advance(seconds: Long) {
        clock.seconds += seconds
    }
}

internal class ToolOutcome(val isError: Boolean, val text: String) {
    val json: JsonObject get() = Json.parseToJsonElement(text).jsonObject
}

/** Stores a signed event of [author] on [relays], as if another client had published it. */
internal suspend fun FakeRelays.seed(author: SecretKey, template: EventTemplate): Event =
    LocalKeySigner(author).sign(template).also { stored += it }

internal fun Event.has(vararg tag: String) = tag.toList() in tags
