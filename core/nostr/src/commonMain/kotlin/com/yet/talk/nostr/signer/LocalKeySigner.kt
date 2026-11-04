package com.yet.talk.nostr.signer

import com.yet.talk.nostr.crypto.Schnorr
import com.yet.talk.nostr.crypto.secureRandomBytes
import com.yet.talk.nostr.encoding.hexToBytes
import com.yet.talk.nostr.encoding.toHex
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventHash
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.key.SecretKey

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
