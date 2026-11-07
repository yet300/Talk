package com.yet.talk.nostr.nip

import com.yet.talk.nostr.crypto.Nip49Primitives
import com.yet.talk.nostr.crypto.secureRandomBytes
import com.yet.talk.nostr.encoding.Bech32

/**
 * NIP-49 private key encryption (`ncryptsec`): scrypt (log_n, r = 8, p = 1) of the NFKC password, then
 * XChaCha20-Poly1305 with the key-security byte as associated data. Talk uses it for the web's app lock
 * (decision 24); Android and iOS use the device lock instead, so [isSupported] is false there.
 */
object Nip49 {
    const val HRP = "ncryptsec"
    const val DEFAULT_LOG_N = 16
    private const val VERSION: Byte = 0x02

    /** NIP-49 key security: unknown whether the key was ever handled insecurely (it may have been exported). */
    private const val KEY_SECURITY_UNKNOWN: Byte = 0x02

    val isSupported: Boolean get() = Nip49Primitives.isSupported

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun encrypt(secretKeyHex: String, password: String, logN: Int = DEFAULT_LOG_N): String {
        val salt = secureRandomBytes(16)
        val nonce = secureRandomBytes(24)
        val key = Nip49Primitives.scrypt(Nip49Primitives.normalize(password), salt, logN)
        val ad = byteArrayOf(KEY_SECURITY_UNKNOWN)
        val ciphertext = Nip49Primitives.seal(key, nonce, ad, secretKeyHex.hexToByteArray())
        return Bech32.encode(HRP, byteArrayOf(VERSION, logN.toByte()) + salt + nonce + ad + ciphertext)
    }

    /** The secret key (hex), or null for a wrong password or a malformed `ncryptsec`. */
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun decrypt(ncryptsec: String, password: String): String? {
        val (hrp, data) = Bech32.decode(ncryptsec.trim()) ?: return null
        if (hrp != HRP || data.size != 91 || data[0] != VERSION) return null
        val logN = data[1].toInt() and 0xff
        val salt = data.copyOfRange(2, 18)
        val nonce = data.copyOfRange(18, 42)
        val ad = data.copyOfRange(42, 43)
        val ciphertext = data.copyOfRange(43, 91)
        val key = Nip49Primitives.scrypt(Nip49Primitives.normalize(password), salt, logN)
        return Nip49Primitives.open(key, nonce, ad, ciphertext)?.toHexString()
    }
}
