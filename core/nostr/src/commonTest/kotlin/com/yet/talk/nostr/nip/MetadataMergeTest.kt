package com.yet.talk.nostr.nip

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class MetadataMergeTest {

    private fun merge(previous: String?, vararg changes: Pair<String, String?>): JsonObject =
        Json.parseToJsonElement(mergeMetadataContent(previous, mapOf(*changes))).jsonObject

    @Test
    fun keepsFieldsItDoesNotChange() {
        val merged = merge("""{"name":"old","lud16":"me@ln.example","custom":{"a":1}}""", "name" to "new")
        assertEquals(JsonPrimitive("new"), merged["name"])
        assertEquals(JsonPrimitive("me@ln.example"), merged["lud16"])
        assertEquals(Json.parseToJsonElement("""{"a":1}"""), merged["custom"])
    }

    @Test
    fun nullRemovesAKeyAndDisplayNameReplacesTheLegacyKey() {
        val merged = merge("""{"about":"bye","displayName":"Legacy"}""", "about" to null, "display_name" to "Shown")
        assertEquals<Map<String, Any>>(mapOf("display_name" to JsonPrimitive("Shown")), merged)
    }

    @Test
    fun startsFreshWithoutAPreviousProfile() {
        assertEquals<Map<String, Any>>(mapOf("name" to JsonPrimitive("a")), merge(null, "name" to "a", "about" to null))
        assertEquals<Map<String, Any>>(mapOf("name" to JsonPrimitive("a")), merge("not json", "name" to "a"))
    }
}
