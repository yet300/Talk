package com.yet.talk.nostr.nip

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.message.NostrJson

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
