package com.yet.talk.nostr.nip

import com.yet.talk.nostr.Event
import com.yet.talk.nostr.EventTemplate
import com.yet.talk.nostr.NostrJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
