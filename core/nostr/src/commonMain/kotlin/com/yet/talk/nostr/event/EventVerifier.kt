package com.yet.talk.nostr.event

import com.yet.talk.nostr.crypto.Schnorr
import com.yet.talk.nostr.encoding.hexToBytes
import com.yet.talk.nostr.encoding.isHex

/** Checks that an event is untampered: the id matches the content and the signature matches the id. */
object EventVerifier {
    fun verify(event: Event): Boolean =
        event.id.isHex(32) && event.pubkey.isHex(32) && event.sig.isHex(64) &&
            event.computeId() == event.id &&
            Schnorr.verify(event.sig.hexToBytes(), event.id.hexToBytes(), event.pubkey.hexToBytes())
}
