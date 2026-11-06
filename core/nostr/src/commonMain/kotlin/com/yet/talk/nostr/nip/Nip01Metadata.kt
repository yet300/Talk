package com.yet.talk.nostr.nip

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.message.NostrJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Kind 0 profile metadata (NIP-01, NIP-24). Unknown fields are ignored when reading. */
@Serializable
data class Metadata(
    val name: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val about: String? = null,
    val picture: String? = null,
    val banner: String? = null,
    val website: String? = null,
    val nip05: String? = null,
    val lud16: String? = null,
) {
    fun toTemplate(createdAt: Long, extraTags: List<List<String>> = emptyList()) =
        EventTemplate(kind = Kinds.METADATA, content = NostrJson.encodeToString(serializer(), this), tags = extraTags, createdAt = createdAt)

    companion object {
        /** Null when the content is not a JSON object. Clients still write the legacy `displayName` key, so it is read too. */
        fun parse(event: Event): Metadata? = runCatching {
            val metadata = NostrJson.decodeFromString(serializer(), event.content)
            if (metadata.displayName != null) {
                metadata
            } else {
                metadata.copy(displayName = NostrJson.decodeFromString(LegacyDisplayName.serializer(), event.content).displayName)
            }
        }.getOrNull()
    }

    @Serializable
    private class LegacyDisplayName(val displayName: String? = null)
}

/**
 * The content of a new kind 0: [previous] (the latest kind 0 content, if any) with [changes] applied. A null
 * value removes its key; keys [changes] does not mention (`lud16`, fields of other apps) are kept as they are.
 * Setting `display_name` also drops the legacy `displayName` key, so the two cannot disagree.
 */
fun mergeMetadataContent(previous: String?, changes: Map<String, String?>): String {
    val base = previous?.let { runCatching { NostrJson.parseToJsonElement(it) as? JsonObject }.getOrNull() }.orEmpty()
    val merged = buildMap {
        putAll(base)
        changes.forEach { (key, value) -> if (value == null) remove(key) else put(key, JsonPrimitive(value)) }
        if ("display_name" in changes) remove("displayName")
    }
    return NostrJson.encodeToString(JsonObject.serializer(), JsonObject(merged))
}
