package com.yet.talk.nostr.message

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** A NIP-01 subscription filter. [tags] maps a single-letter tag name to accepted values (`'e'` → `#e`). */
data class Filter(
    val ids: List<String>? = null,
    val authors: List<String>? = null,
    val kinds: List<Int>? = null,
    val tags: Map<Char, List<String>> = emptyMap(),
    val since: Long? = null,
    val until: Long? = null,
    val limit: Int? = null,
) {
    fun toJson(): JsonObject = buildJsonObject {
        ids?.let { put("ids", it.toJsonArray()) }
        authors?.let { put("authors", it.toJsonArray()) }
        kinds?.let { kinds -> put("kinds", JsonArray(kinds.map(::JsonPrimitive))) }
        tags.forEach { (name, values) -> put("#$name", values.toJsonArray()) }
        since?.let { put("since", it) }
        until?.let { put("until", it) }
        limit?.let { put("limit", it) }
    }

    private fun List<String>.toJsonArray() = JsonArray(map(::JsonPrimitive))
}
