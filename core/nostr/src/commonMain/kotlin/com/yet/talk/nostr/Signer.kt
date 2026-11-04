package com.yet.talk.nostr

import com.yet.talk.nostr.crypto.Schnorr
import com.yet.talk.nostr.crypto.secureRandomBytes

/**
 * Signs events for one identity. `suspend` so that asynchronous signers (NIP-07 extensions,
 * Amber, remote signers) fit the same contract as a local key.
 */
interface Signer {
    suspend fun publicKey(): String

    suspend fun sign(template: EventTemplate): Event
}

class LocalKeySigner(private val secretKey: SecretKey) : Signer {

    override suspend fun publicKey(): String = secretKey.publicKey

    override suspend fun sign(template: EventTemplate): Event {
        val pubkey = secretKey.publicKey
        val id = EventHash.id(pubkey, template.createdAt, template.kind, template.tags, template.content)
        val signature = Schnorr.sign(id.hexToBytes(), secretKey.bytes, auxRand = secureRandomBytes(32))
        return Event(
            id = id,
            pubkey = pubkey,
            createdAt = template.createdAt,
            kind = template.kind,
            tags = template.tags,
            content = template.content,
            sig = signature.toHex(),
        )
    }
}

/** Checks that an event is untampered: the id matches the content and the signature matches the id. */
object EventVerifier {
    fun verify(event: Event): Boolean =
        event.id.isHex(32) && event.pubkey.isHex(32) && event.sig.isHex(64) &&
            event.computeId() == event.id &&
            Schnorr.verify(event.sig.hexToBytes(), event.id.hexToBytes(), event.pubkey.hexToBytes())
}
