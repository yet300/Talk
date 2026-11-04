package com.yet.talk.nostr.relay

import com.yet.talk.nostr.Event
import com.yet.talk.nostr.NostrJson
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
) {
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
                    stored.forEach { push(subscriptionId, it) }
                    inbound.send(JsonArray(listOf(JsonPrimitive("EOSE"), JsonPrimitive(subscriptionId))).toString())
                }
                "EVENT" -> {
                    val id = message[1].jsonObject["id"]!!.jsonPrimitive.content
                    inbound.send(JsonArray(listOf(JsonPrimitive("OK"), JsonPrimitive(id), JsonPrimitive(acceptEvents), JsonPrimitive(if (acceptEvents) "" else "blocked: test"))).toString())
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

/** Routes each URL to its [FakeRelay]; unknown URLs fail to connect. */
class FakeTransport(private val relays: Map<String, FakeRelay>) : RelayTransport {
    override suspend fun connect(url: String): RelaySocket {
        val relay = relays[url] ?: throw IllegalStateException("unreachable: $url")
        return relay.Socket().also { relay.sockets += it }
    }
}
