package com.yet.talk.nostr.nip

import com.yet.talk.nostr.crypto.ChaCha20
import com.yet.talk.nostr.crypto.Hmac
import com.yet.talk.nostr.crypto.platformSharedX
import com.yet.talk.nostr.crypto.secureRandomBytes
import com.yet.talk.nostr.encoding.hexToBytes
import kotlin.io.encoding.Base64

/**
 * NIP-44 v2 encryption (secp256k1 ECDH, HKDF-SHA256, ChaCha20, HMAC-SHA256, padded lengths), as NIP-17 chats use
 * it. Checked against the official test vectors (`Nip44Test`).
 */
object Nip44 {
    private const val VERSION: Byte = 2
    private val SALT = "nip44-v2".encodeToByteArray()

    /** The key two parties share: [secretKeyHex] and [publicKeyHex] (x-only) are hex. */
    fun conversationKey(secretKeyHex: String, publicKeyHex: String): ByteArray =
        Hmac.hkdfExtract(SALT, platformSharedX(secretKeyHex.hexToBytes(), publicKeyHex.hexToBytes()))

    fun encrypt(plaintext: String, conversationKey: ByteArray, nonce: ByteArray = secureRandomBytes(32)): String {
        val (chachaKey, chachaNonce, hmacKey) = messageKeys(conversationKey, nonce)
        val ciphertext = ChaCha20.xor(chachaKey, chachaNonce, pad(plaintext))
        val mac = Hmac.sha256(hmacKey, nonce, ciphertext)
        return Base64.encode(byteArrayOf(VERSION) + nonce + ciphertext + mac)
    }

    /** Throws [IllegalArgumentException] for an unknown version, a bad length or a wrong MAC. */
    fun decrypt(payload: String, conversationKey: ByteArray): String {
        require(payload.isNotEmpty() && payload[0] != '#') { "unknown encryption version" }
        require(payload.length in 132..87472) { "invalid payload size" }
        val data = Base64.decode(payload)
        require(data.size in 99..65603) { "invalid data size" }
        require(data[0] == VERSION) { "unknown encryption version ${data[0]}" }
        val nonce = data.copyOfRange(1, 33)
        val ciphertext = data.copyOfRange(33, data.size - 32)
        val mac = data.copyOfRange(data.size - 32, data.size)
        val (chachaKey, chachaNonce, hmacKey) = messageKeys(conversationKey, nonce)
        require(Hmac.equalsConstantTime(mac, Hmac.sha256(hmacKey, nonce, ciphertext))) { "invalid MAC" }
        return unpad(ChaCha20.xor(chachaKey, chachaNonce, ciphertext))
    }

    internal fun messageKeys(conversationKey: ByteArray, nonce: ByteArray): Triple<ByteArray, ByteArray, ByteArray> {
        require(conversationKey.size == 32 && nonce.size == 32)
        val keys = Hmac.hkdfExpand(conversationKey, nonce, 76)
        return Triple(keys.copyOfRange(0, 32), keys.copyOfRange(32, 44), keys.copyOfRange(44, 76))
    }

    internal fun paddedLength(length: Int): Int {
        if (length <= 32) return 32
        val nextPower = 1 shl (32 - (length - 1).countLeadingZeroBits())
        val chunk = if (nextPower <= 256) 32 else nextPower / 8
        return chunk * ((length - 1) / chunk + 1)
    }

    private fun pad(plaintext: String): ByteArray {
        val bytes = plaintext.encodeToByteArray()
        require(bytes.size in 1..65535) { "invalid plaintext length" }
        val out = ByteArray(2 + paddedLength(bytes.size))
        out[0] = (bytes.size ushr 8).toByte()
        out[1] = bytes.size.toByte()
        bytes.copyInto(out, 2)
        return out
    }

    private fun unpad(padded: ByteArray): String {
        val length = ((padded[0].toInt() and 0xff) shl 8) or (padded[1].toInt() and 0xff)
        require(length in 1..65535 && padded.size == 2 + paddedLength(length)) { "invalid padding" }
        return padded.copyOfRange(2, 2 + length).decodeToString()
    }
}
