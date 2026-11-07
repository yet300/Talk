package com.yet.talk.nostr.crypto

/** scrypt and XChaCha20-Poly1305 for [com.yet.talk.nostr.nip.Nip49]; only the web has them (`@noble`). */
internal expect object Nip49Primitives {
    val isSupported: Boolean

    /** The password's UTF-8 bytes after Unicode NFKC normalisation. */
    fun normalize(password: String): ByteArray

    /** scrypt with N = 2^[logN], r = 8, p = 1, 32 bytes out. */
    suspend fun scrypt(password: ByteArray, salt: ByteArray, logN: Int): ByteArray

    fun seal(key: ByteArray, nonce: ByteArray, ad: ByteArray, plaintext: ByteArray): ByteArray

    /** Null when the tag does not match (a wrong password). */
    fun open(key: ByteArray, nonce: ByteArray, ad: ByteArray, ciphertext: ByteArray): ByteArray?
}
