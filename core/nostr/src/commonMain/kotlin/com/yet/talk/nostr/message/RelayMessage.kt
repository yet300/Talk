package com.yet.talk.nostr.message

import com.yet.talk.nostr.event.Event
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

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
