package com.yet.talk.nostr.signer

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate

/**
 * Signs events for one identity. `suspend` so that asynchronous signers (NIP-07 extensions,
 * Amber, remote signers) fit the same contract as a local key.
 */
interface Signer {
    suspend fun publicKey(): String

    suspend fun sign(template: EventTemplate): Event
}
