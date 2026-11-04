package com.yet.talk.api

import com.yet.talk.nostr.Event
import com.yet.talk.nostr.nip.Imeta
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.Metadata
import com.yet.talk.nostr.nip.RelayListEntry
import com.yet.talk.nostr.nip.ThreadReference
import com.yet.talk.nostr.nip.bookmarkedEventIds
import com.yet.talk.nostr.nip.contactPubkeys
import com.yet.talk.nostr.nip.firstValue
import com.yet.talk.nostr.nip.imeta
import com.yet.talk.nostr.nip.relayListEntries
import com.yet.talk.nostr.nip.values
import com.yet.talk.nostr.nip.withName

/** A verified relay event, interpreted by Talk's schema. */
sealed interface TalkEvent {
    val event: Event

    /** A labelled kind 1: a top-level post when [thread] is not a reply. */
    data class Post(
        override val event: Event,
        val thread: ThreadReference,
        val quoteId: String?,
        val images: List<Imeta>,
    ) : TalkEvent

    data class Revision(override val event: Event, val postId: String, val number: Int, val images: List<Imeta>) : TalkEvent

    /** A like or a repost of [postId]. Reactions from any client count, not only Talk's. */
    data class Reaction(override val event: Event, val postId: String, val type: Type) : TalkEvent {
        enum class Type { Like, Repost }
    }

    data class Profile(override val event: Event, val metadata: Metadata) : TalkEvent

    data class Deletion(override val event: Event, val eventIds: List<String>) : TalkEvent

    data class Contacts(override val event: Event, val pubkeys: List<String>) : TalkEvent

    data class Bookmarks(override val event: Event, val eventIds: List<String>) : TalkEvent

    data class RelayList(override val event: Event, val relays: List<RelayListEntry>) : TalkEvent

    companion object {
        /** Null for events Talk does not use, including kind 1 and 1010 without the Talk label. */
        fun parse(event: Event): TalkEvent? = when (event.kind) {
            Kinds.TEXT_NOTE -> if (TalkSchema.isTalkEvent(event)) {
                Post(event, ThreadReference.parse(event.tags), event.tags.firstValue("q"), event.imeta())
            } else {
                null
            }
            TalkSchema.KIND_REVISION -> {
                val postId = event.tags.withName("e").firstOrNull()?.getOrNull(1)
                val number = event.tags.firstValue(TalkSchema.REVISION_TAG)?.toIntOrNull()
                if (TalkSchema.isTalkEvent(event) && postId != null && number != null && number >= 2) {
                    Revision(event, postId, number, event.imeta())
                } else {
                    null
                }
            }
            // NIP-25: the target is the last `e` tag; "-" is a dislike, which Talk does not show.
            Kinds.REACTION -> event.tags.values("e").lastOrNull()
                ?.takeIf { event.content != "-" }
                ?.let { Reaction(event, it, Reaction.Type.Like) }
            Kinds.REPOST, Kinds.GENERIC_REPOST -> event.tags.firstValue("e")?.let { Reaction(event, it, Reaction.Type.Repost) }
            Kinds.METADATA -> Metadata.parse(event)?.let { Profile(event, it) }
            Kinds.DELETION -> Deletion(event, event.tags.values("e"))
            Kinds.CONTACTS -> Contacts(event, event.contactPubkeys())
            Kinds.BOOKMARKS -> Bookmarks(event, event.bookmarkedEventIds())
            Kinds.RELAY_LIST -> RelayList(event, event.relayListEntries())
            else -> null
        }
    }
}
