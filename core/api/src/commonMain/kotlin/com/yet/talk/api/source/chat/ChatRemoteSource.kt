package com.yet.talk.api.source.chat

import com.yet.talk.nostr.event.Event
import kotlinx.coroutines.flow.Flow

/** Gift wraps and inbox relay lists (NIP-17) on relays. */
interface ChatRemoteSource {
    /** Each pubkey's newest kind 10050 relays; absent when they have none. */
    suspend fun inboxRelays(pubkeys: Collection<String>): Map<String, List<String>>

    /** Publishes [event] (a wrap, or the viewer's 10050) to [relays]; true when at least one accepts it. */
    suspend fun publish(event: Event, relays: Collection<String>): Boolean

    /** Gift wraps addressed to [pubkey] since [since], stored then live, from [relays] (which may ask for AUTH). */
    fun wraps(pubkey: String, since: Long?, relays: Collection<String>): Flow<Event>

    companion object {
        /**
         * Inbox relays for someone who has none yet: auth.nostr1.com serves wraps only to their recipient
         * (NIP-42), relay.damus.io adds reach.
         */
        val DEFAULT_INBOX = listOf("wss://auth.nostr1.com", "wss://relay.damus.io")

        /** Where others' inbox lists are looked for, beyond the viewer's read relays. */
        val INDEXERS = listOf("wss://purplepag.es")
    }
}
