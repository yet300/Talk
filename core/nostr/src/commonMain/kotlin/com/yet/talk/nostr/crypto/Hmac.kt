package com.yet.talk.nostr.crypto

import org.kotlincrypto.hash.sha2.SHA256

/** HMAC-SHA256 (RFC 2104) and HKDF-SHA256 (RFC 5869), for NIP-44. */
internal object Hmac {
    private const val BLOCK = 64

    fun sha256(key: ByteArray, vararg parts: ByteArray): ByteArray {
        val k = if (key.size > BLOCK) SHA256().digest(key) else key
        val padded = k.copyOf(BLOCK)
        val inner = SHA256().apply {
            update(ByteArray(BLOCK) { (padded[it].toInt() xor 0x36).toByte() })
            parts.forEach { update(it) }
        }.digest()
        return SHA256().apply {
            update(ByteArray(BLOCK) { (padded[it].toInt() xor 0x5c).toByte() })
            update(inner)
        }.digest()
    }

    fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray = sha256(salt, ikm)

    fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val out = ByteArray(length)
        var previous = ByteArray(0)
        var written = 0
        var counter = 1
        while (written < length) {
            previous = sha256(prk, previous, info, byteArrayOf(counter++.toByte()))
            val take = minOf(previous.size, length - written)
            previous.copyInto(out, written, 0, take)
            written += take
        }
        return out
    }

    /** Compares without leaking where they differ. */
    fun equalsConstantTime(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}

/** HMAC-SHA256, for deriving a purpose-specific key from a secret (a PRF, as in HKDF-Expand). */
fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray = Hmac.sha256(key, data)
