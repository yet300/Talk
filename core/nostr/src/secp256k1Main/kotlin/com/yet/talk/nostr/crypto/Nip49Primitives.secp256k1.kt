package com.yet.talk.nostr.crypto

/** Android and iOS lock the app with the device lock (decision 24), so NIP-49 is not built there. */
internal actual object Nip49Primitives {
    actual val isSupported: Boolean = false

    actual fun normalize(password: String): ByteArray = unsupported()

    actual suspend fun scrypt(password: ByteArray, salt: ByteArray, logN: Int): ByteArray = unsupported()

    actual fun seal(key: ByteArray, nonce: ByteArray, ad: ByteArray, plaintext: ByteArray): ByteArray = unsupported()

    actual fun open(key: ByteArray, nonce: ByteArray, ad: ByteArray, ciphertext: ByteArray): ByteArray? = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("NIP-49 is only used on the web")
}
