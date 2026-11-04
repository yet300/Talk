package com.yet.talk.nostr.key

import com.yet.talk.nostr.crypto.Schnorr
import com.yet.talk.nostr.crypto.secureRandomBytes
import com.yet.talk.nostr.encoding.hexToBytes
import com.yet.talk.nostr.encoding.isHex
import com.yet.talk.nostr.encoding.toHex

/**
 * A secp256k1 secret key. It never prints itself: [toString] is redacted, so logging a signer or a
 * data class that holds one cannot leak it. Persist it only through [toHex] into encrypted storage.
 */
class SecretKey private constructor(internal val bytes: ByteArray) {

    /** The x-only public key, hex. */
    val publicKey: String by lazy { Schnorr.publicKey(bytes).toHex() }

    fun toHex(): String = bytes.toHex()

    override fun toString(): String = "SecretKey(redacted)"

    override fun equals(other: Any?): Boolean = other is SecretKey && other.bytes.contentEquals(bytes)

    override fun hashCode(): Int = bytes.contentHashCode()

    companion object {
        fun generate(): SecretKey {
            while (true) {
                val candidate = secureRandomBytes(32)
                if (Schnorr.isValidSecretKey(candidate)) return SecretKey(candidate)
            }
        }

        /** Null when [hex] is not 32 bytes of hex or not a valid secp256k1 scalar. */
        fun fromHex(hex: String): SecretKey? = if (hex.isHex(32)) fromBytes(hex.hexToBytes()) else null

        internal fun fromBytes(bytes: ByteArray): SecretKey? =
            if (Schnorr.isValidSecretKey(bytes)) SecretKey(bytes.copyOf()) else null
    }
}
