package com.yet.talk.nostr.nip

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate

/** NIP-51: the public bookmark list (kind 10003) of event ids. */
fun bookmarkList(eventIds: Collection<String>, createdAt: Long) =
    EventTemplate(kind = Kinds.BOOKMARKS, content = "", tags = eventIds.distinct().map { Tags.event(it) }, createdAt = createdAt)

fun Event.bookmarkedEventIds(): List<String> = tags.values("e").distinct()
