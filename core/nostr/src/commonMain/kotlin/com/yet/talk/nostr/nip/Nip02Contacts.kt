package com.yet.talk.nostr.nip

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate

/** NIP-02: the full follow list (it replaces the previous one). */
fun contactList(pubkeys: Collection<String>, createdAt: Long) =
    EventTemplate(kind = Kinds.CONTACTS, content = "", tags = pubkeys.distinct().map { Tags.pubkey(it) }, createdAt = createdAt)

fun Event.contactPubkeys(): List<String> = tags.values("p").distinct()
