package com.yet.talk.nostr

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

internal val NostrJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

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
}

private fun frame(type: String, vararg parts: kotlinx.serialization.json.JsonElement): String =
    JsonArray(listOf(JsonPrimitive(type)) + parts).toString()

/** Messages a relay sends to a client. Unknown or malformed frames parse to null and are ignored. */
sealed interface RelayMessage {

    sealed interface ForSubscription : RelayMessage {
        val subscriptionId: String
    }

    data class EventMessage(override val subscriptionId: String, val event: Event) : ForSubscription

    data class EndOfStoredEvents(override val subscriptionId: String) : ForSubscription

    data class Closed(override val subscriptionId: String, val message: String) : ForSubscription

    data class CountResult(override val subscriptionId: String, val count: Long) : ForSubscription

    data class Ok(val eventId: String, val accepted: Boolean, val message: String) : RelayMessage

    data class Notice(val message: String) : RelayMessage

    data class Auth(val challenge: String) : RelayMessage

    companion object {
        fun parse(text: String): RelayMessage? = runCatching {
            val parts = NostrJson.parseToJsonElement(text).jsonArray
            fun string(index: Int) = parts.getOrNull(index)?.jsonPrimitive?.contentOrNull
            when (string(0)) {
                "EVENT" -> EventMessage(
                    subscriptionId = string(1)!!,
                    event = NostrJson.decodeFromJsonElement(Event.serializer(), parts[2].jsonObject),
                )
                "EOSE" -> EndOfStoredEvents(string(1)!!)
                "CLOSED" -> Closed(string(1)!!, string(2).orEmpty())
                "COUNT" -> CountResult(string(1)!!, (parts[2] as JsonObject)["count"]!!.jsonPrimitive.longOrNull!!)
                "OK" -> Ok(string(1)!!, parts[2].jsonPrimitive.booleanOrNull!!, string(3).orEmpty())
                "NOTICE" -> Notice(string(1).orEmpty())
                "AUTH" -> Auth(string(1)!!)
                else -> null
            }
        }.getOrNull()
    }
}
