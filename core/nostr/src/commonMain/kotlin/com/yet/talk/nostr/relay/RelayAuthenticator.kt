package com.yet.talk.nostr.relay

import com.yet.talk.nostr.event.Event

/**
 * Answers a relay's NIP-42 `AUTH` challenge with a signed kind 22242 event, or null to stay anonymous (signed out).
 * Inbox relays need it to hand out a user's gift wraps (NIP-17) only to that user.
 */
fun interface RelayAuthenticator {
    suspend fun authenticate(relayUrl: String, challenge: String): Event?
}
