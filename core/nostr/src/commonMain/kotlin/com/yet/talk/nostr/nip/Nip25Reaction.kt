package com.yet.talk.nostr.nip

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate

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
