package com.yet.talk.nostr.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES

/**
 * AES-256-GCM for encrypted chat files (NIP-17 kind 15): a fresh key and nonce per file, the 128-bit tag appended
 * to the ciphertext. JDK on Android, WebCrypto on the web, CryptoKit on iOS.
 */
object AesGcm {
    const val KEY_SIZE = 32
    const val NONCE_SIZE = 12

    class Sealed(val ciphertext: ByteArray, val key: ByteArray, val nonce: ByteArray)

    suspend fun encrypt(plaintext: ByteArray): Sealed {
        val key = secureRandomBytes(KEY_SIZE)
        val nonce = secureRandomBytes(NONCE_SIZE)
        return Sealed(encrypt(plaintext, key, nonce), key, nonce)
    }

    @OptIn(DelicateCryptographyApi::class)
    suspend fun encrypt(plaintext: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray =
        cipher(key).encryptWithIv(nonce, plaintext)

    /** Throws when the tag does not match (a tampered or wrong file). */
    @OptIn(DelicateCryptographyApi::class)
    suspend fun decrypt(ciphertext: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray =
        cipher(key).decryptWithIv(nonce, ciphertext)

    private suspend fun cipher(key: ByteArray) =
        CryptographyProvider.Default.get(AES.GCM).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, key).cipher()
}
