package com.yet.talk.api.schema

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.firstValue
import com.yet.talk.nostr.nip.withName

/** bitchat's location channels, tag for tag: kind 20000 messages and kind 20001 presence. No Talk label. */
object ChannelEvents {
    /** `["g", area]`, then `["n", nickname]` if any, then `["t", "teleport"]` when not there. */
    fun message(geohash: String, content: String, nickname: String?, teleported: Boolean, createdAt: Long) = EventTemplate(
        kind = Kinds.GEOHASH_MESSAGE,
        content = content,
        tags = listOfNotNull(
            listOf("g", geohash),
            nickname?.trim()?.takeIf(String::isNotEmpty)?.let { listOf("n", it) },
            if (teleported) listOf("t", "teleport") else null,
        ),
        createdAt = createdAt,
    )

    /** Presence: only `["g", area]`, empty content. */
    fun presence(geohash: String, createdAt: Long) = EventTemplate(Kinds.GEOHASH_PRESENCE, "", listOf(listOf("g", geohash)), createdAt)

    /** The area an event is for (its first `g` tag). */
    fun geohash(event: Event): String? = event.tags.firstValue("g")?.lowercase()

    fun nickname(event: Event): String? = event.tags.firstValue("n")?.trim()?.takeIf(String::isNotEmpty)

    fun isTeleported(event: Event): Boolean = event.tags.withName("t").any { it.getOrNull(1) == "teleport" }
}
