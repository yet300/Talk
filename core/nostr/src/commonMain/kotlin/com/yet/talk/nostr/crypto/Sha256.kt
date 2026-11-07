package com.yet.talk.nostr.crypto

import org.kotlincrypto.hash.sha2.SHA256

/** SHA-256, as Blossom and NIP-17 file messages name blobs. */
object Sha256 {
    fun hex(bytes: ByteArray): String = SHA256().digest(bytes).toHexString()
}
