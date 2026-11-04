package com.yet.talk.nostr.nip

import com.yet.talk.nostr.Event
import com.yet.talk.nostr.EventTemplate
import com.yet.talk.nostr.NostrJson

/** NIP-25: a like (`+`) on [target]. */
fun reaction(target: Event, createdAt: Long, content: String = "+", relayHint: String = "", extraTags: List<List<String>> = emptyList()) =
    EventTemplate(
        kind = Kinds.REACTION,
        content = content,
        tags = listOf(
            Tags.event(target.id, relayHint, author = target.pubkey),
            Tags.pubkey(target.pubkey),
            Tags.kind(target.kind),
        ) + extraTags,
        createdAt = createdAt,
    )

/** NIP-18: a repost. Kind 6 for kind 1 targets, kind 16 (with a `k` tag) for anything else. */
fun repost(target: Event, createdAt: Long, relayHint: String = "", extraTags: List<List<String>> = emptyList()) =
    EventTemplate(
        kind = if (target.kind == Kinds.TEXT_NOTE) Kinds.REPOST else Kinds.GENERIC_REPOST,
        content = NostrJson.encodeToString(Event.serializer(), target),
        tags = listOfNotNull(
            Tags.event(target.id, relayHint),
            Tags.pubkey(target.pubkey),
            Tags.kind(target.kind).takeIf { target.kind != Kinds.TEXT_NOTE },
        ) + extraTags,
        createdAt = createdAt,
    )

/** NIP-09: asks relays to delete the author's own [events]. */
fun deletion(events: List<Event>, createdAt: Long, reason: String = "") =
    EventTemplate(
        kind = Kinds.DELETION,
        content = reason,
        tags = events.map { Tags.event(it.id) } + events.map { it.kind }.distinct().map(Tags::kind),
        createdAt = createdAt,
    )

/** NIP-02: the full follow list (it replaces the previous one). */
fun contactList(pubkeys: Collection<String>, createdAt: Long) =
    EventTemplate(kind = Kinds.CONTACTS, content = "", tags = pubkeys.distinct().map { Tags.pubkey(it) }, createdAt = createdAt)

fun Event.contactPubkeys(): List<String> = tags.values("p").distinct()

/** NIP-51: the public bookmark list (kind 10003) of event ids. */
fun bookmarkList(eventIds: Collection<String>, createdAt: Long) =
    EventTemplate(kind = Kinds.BOOKMARKS, content = "", tags = eventIds.distinct().map { Tags.event(it) }, createdAt = createdAt)

fun Event.bookmarkedEventIds(): List<String> = tags.values("e").distinct()

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

/** NIP-92 media attachment. */
data class Imeta(
    val url: String,
    val mimeType: String? = null,
    val sha256: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val alt: String? = null,
) {
    fun toTag(): List<String> = listOfNotNull(
        "imeta",
        "url $url",
        mimeType?.let { "m $it" },
        sha256?.let { "x $it" },
        if (width != null && height != null) "dim ${width}x$height" else null,
        alt?.let { "alt $it" },
    )

    companion object {
        fun parse(tag: List<String>): Imeta? {
            if (tag.firstOrNull() != "imeta") return null
            val fields = tag.drop(1).mapNotNull { entry ->
                val space = entry.indexOf(' ')
                if (space <= 0) null else entry.substring(0, space) to entry.substring(space + 1)
            }.toMap()
            val url = fields["url"] ?: return null
            val dimensions = fields["dim"]?.split('x')
            return Imeta(
                url = url,
                mimeType = fields["m"],
                sha256 = fields["x"],
                width = dimensions?.getOrNull(0)?.toDoubleOrNull()?.toInt(),
                height = dimensions?.getOrNull(1)?.toDoubleOrNull()?.toInt(),
                alt = fields["alt"],
            )
        }
    }
}

fun Event.imeta(): List<Imeta> = tags.mapNotNull(Imeta::parse)
