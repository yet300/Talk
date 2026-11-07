package com.yet.talk.nostr.message

import com.yet.talk.nostr.event.Event
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

/** Messages a client sends to a relay (NIP-01, NIP-45). */
sealed interface ClientMessage {
    fun toJson(): String

    data class Req(val subscriptionId: String, val filters: List<Filter>) : ClientMessage {
        override fun toJson() = frame("REQ", JsonPrimitive(subscriptionId), *filters.map(Filter::toJson).toTypedArray())
    }

    data class Count(val subscriptionId: String, val filters: List<Filter>) : ClientMessage {
        override fun toJson() = frame("COUNT", JsonPrimitive(subscriptionId), *filters.map(Filter::toJson).toTypedArray())
    }

    data class Close(val subscriptionId: String) : ClientMessage {
        override fun toJson() = frame("CLOSE", JsonPrimitive(subscriptionId))
    }

    data class Publish(val event: Event) : ClientMessage {
        override fun toJson() = frame("EVENT", NostrJson.encodeToJsonElement(Event.serializer(), event))
    }

    /** NIP-42: the signed answer to a relay's challenge. */
    data class Auth(val event: Event) : ClientMessage {
        override fun toJson() = frame("AUTH", NostrJson.encodeToJsonElement(Event.serializer(), event))
    }
}

private fun frame(type: String, vararg parts: kotlinx.serialization.json.JsonElement): String =
    JsonArray(listOf(JsonPrimitive(type)) + parts).toString()
