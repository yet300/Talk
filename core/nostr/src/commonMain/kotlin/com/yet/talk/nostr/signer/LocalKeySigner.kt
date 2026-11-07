package com.yet.talk.nostr.signer

import com.yet.talk.nostr.crypto.Schnorr
import com.yet.talk.nostr.crypto.secureRandomBytes
import com.yet.talk.nostr.encoding.hexToBytes
import com.yet.talk.nostr.encoding.toHex
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventHash
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.nip.Nip44

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

    override suspend fun nip44Encrypt(plaintext: String, peerPubkey: String): String =
        Nip44.encrypt(plaintext, conversationKey(peerPubkey))

    override suspend fun nip44Decrypt(payload: String, peerPubkey: String): String =
        Nip44.decrypt(payload, conversationKey(peerPubkey))

    // Derived per call: the shared key is as sensitive as the secret key, so it is not cached.
    private fun conversationKey(peerPubkey: String) = Nip44.conversationKey(secretKey.toHex(), peerPubkey)
}
