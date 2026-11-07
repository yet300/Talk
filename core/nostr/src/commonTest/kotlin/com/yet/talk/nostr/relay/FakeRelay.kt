package com.yet.talk.nostr.relay

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.message.NostrJson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** An in-memory relay: answers REQ with [stored] + EOSE, EVENT with OK, COUNT with the stored size. */
class FakeRelay(
    var stored: List<Event> = emptyList(),
    var acceptEvents: Boolean = true,
    var supportsCount: Boolean = true,
    /** NIP-42: when set, REQs are refused with "auth-required" until a kind 22242 answers this challenge. */
    var authChallenge: String? = null,
) {
    var authenticated = false
    val received = mutableListOf<String>()
    val sockets = mutableListOf<Socket>()

    inner class Socket : RelaySocket {
        private val inbound = Channel<String>(Channel.UNLIMITED)
        override val incoming: Flow<String> = inbound.consumeAsFlow()

        override suspend fun send(text: String) {
            received += text
            val message = NostrJson.parseToJsonElement(text).jsonArray
            val type = message[0].jsonPrimitive.content
            when (type) {
                "REQ" -> {
                    val subscriptionId = message[1].jsonPrimitive.content
                    val challenge = authChallenge
                    if (challenge != null && !authenticated) {
                        inbound.send(JsonArray(listOf(JsonPrimitive("AUTH"), JsonPrimitive(challenge))).toString())
                        inbound.send(JsonArray(listOf(JsonPrimitive("CLOSED"), JsonPrimitive(subscriptionId), JsonPrimitive("auth-required: who are you"))).toString())
                        return
                    }
                    stored.forEach { push(subscriptionId, it) }
                    inbound.send(JsonArray(listOf(JsonPrimitive("EOSE"), JsonPrimitive(subscriptionId))).toString())
                }
                "EVENT" -> {
                    val id = message[1].jsonObject["id"]!!.jsonPrimitive.content
                    inbound.send(JsonArray(listOf(JsonPrimitive("OK"), JsonPrimitive(id), JsonPrimitive(acceptEvents), JsonPrimitive(if (acceptEvents) "" else "blocked: test"))).toString())
                }
                "AUTH" -> {
                    val auth = message[1].jsonObject
                    val id = auth["id"]!!.jsonPrimitive.content
                    val tags = auth["tags"]!!.jsonArray.map { tag -> tag.jsonArray.map { it.jsonPrimitive.content } }
                    authenticated = auth["kind"]!!.jsonPrimitive.content == "22242" && listOf("challenge", authChallenge) in tags
                    inbound.send(JsonArray(listOf(JsonPrimitive("OK"), JsonPrimitive(id), JsonPrimitive(authenticated), JsonPrimitive(""))).toString())
                }
                "COUNT" -> {
                    val subscriptionId = message[1].jsonPrimitive.content
                    val reply = if (supportsCount) {
                        JsonArray(listOf(JsonPrimitive("COUNT"), JsonPrimitive(subscriptionId), JsonObject(mapOf("count" to JsonPrimitive(stored.size)))))
                    } else {
                        JsonArray(listOf(JsonPrimitive("CLOSED"), JsonPrimitive(subscriptionId), JsonPrimitive("unsupported: count")))
                    }
                    inbound.send(reply.toString())
                }
            }
        }

        suspend fun push(subscriptionId: String, event: Event) {
            inbound.send(JsonArray(listOf(JsonPrimitive("EVENT"), JsonPrimitive(subscriptionId), NostrJson.encodeToJsonElement(Event.serializer(), event))).toString())
        }

        /** Simulates the relay dropping the connection. */
        fun drop() {
            inbound.close()
        }

        override suspend fun close() {
            inbound.close()
        }
    }
}

/** Routes each URL to its [FakeRelay]; unknown URLs and URLs in [down] fail to connect. */
class FakeTransport(private val relays: Map<String, FakeRelay>) : RelayTransport {
    var down: Set<String> = emptySet()

    override suspend fun connect(url: String): RelaySocket {
        val relay = relays[url]?.takeIf { url !in down } ?: throw IllegalStateException("unreachable: $url")
        return relay.Socket().also { relay.sockets += it }
    }
}
