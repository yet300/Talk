package com.yet.talk.nostr.nip

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate

/** NIP-65 entry. An `r` tag without a marker means both read and write. */
data class RelayListEntry(val url: String, val read: Boolean, val write: Boolean)

fun relayList(entries: Collection<RelayListEntry>, createdAt: Long) =
    EventTemplate(
        kind = Kinds.RELAY_LIST,
        content = "",
        tags = entries.map { entry ->
            when {
                entry.read && entry.write -> listOf("r", entry.url)
                entry.read -> listOf("r", entry.url, "read")
                else -> listOf("r", entry.url, "write")
            }
        },
        createdAt = createdAt,
    )

fun Event.relayListEntries(): List<RelayListEntry> =
    tags.withName("r").mapNotNull { tag ->
        val url = tag.getOrNull(1) ?: return@mapNotNull null
        when (tag.getOrNull(2)) {
            "read" -> RelayListEntry(url, read = true, write = false)
            "write" -> RelayListEntry(url, read = false, write = true)
            else -> RelayListEntry(url, read = true, write = true)
        }
    }
