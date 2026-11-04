package com.yet.talk.nostr.nip

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate

/** NIP-09: asks relays to delete the author's own [events]. */
fun deletion(events: List<Event>, createdAt: Long, reason: String = "") =
    EventTemplate(
        kind = Kinds.DELETION,
        content = reason,
        tags = events.map { Tags.event(it.id) } + events.map { it.kind }.distinct().map(Tags::kind),
        createdAt = createdAt,
    )
