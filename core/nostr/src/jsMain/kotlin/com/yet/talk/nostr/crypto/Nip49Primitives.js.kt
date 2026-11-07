package com.yet.talk.nostr.crypto

import kotlinx.coroutines.await

internal actual object Nip49Primitives {
    actual val isSupported: Boolean = true

    actual fun normalize(password: String): ByteArray =
        (password.asDynamic().normalize("NFKC") as String).encodeToByteArray()

    actual suspend fun scrypt(password: ByteArray, salt: ByteArray, logN: Int): ByteArray {
        val options = js("({})")
        options.N = 1 shl logN
        options.r = 8
        options.p = 1
        options.dkLen = 32
        // 2^logN * 128 * r bytes, plus headroom for noble's check.
        options.maxmem = (1 shl logN) * 128 * 8 * 2
        return scryptAsync(password.toUint8Array(), salt.toUint8Array(), options).await().toByteArray()
    }

    actual fun seal(key: ByteArray, nonce: ByteArray, ad: ByteArray, plaintext: ByteArray): ByteArray =
        xchacha20poly1305(key.toUint8Array(), nonce.toUint8Array(), ad.toUint8Array()).encrypt(plaintext.toUint8Array()).toByteArray()

    actual fun open(key: ByteArray, nonce: ByteArray, ad: ByteArray, ciphertext: ByteArray): ByteArray? =
        runCatching {
            xchacha20poly1305(key.toUint8Array(), nonce.toUint8Array(), ad.toUint8Array()).decrypt(ciphertext.toUint8Array()).toByteArray()
        }.getOrNull()
}
