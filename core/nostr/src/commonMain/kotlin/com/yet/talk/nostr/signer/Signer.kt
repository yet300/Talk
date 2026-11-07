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

    /** NIP-44 v2 encryption to [peerPubkey] with this identity's key (private chats, NIP-17). */
    suspend fun nip44Encrypt(plaintext: String, peerPubkey: String): String

    /** Throws when the payload is not for this identity or was tampered with. */
    suspend fun nip44Decrypt(payload: String, peerPubkey: String): String
}
