@file:JsModule("@noble/ciphers/chacha.js")
@file:JsNonModule

package com.yet.talk.nostr.crypto

import org.khronos.webgl.Uint8Array

internal external fun xchacha20poly1305(key: Uint8Array, nonce: Uint8Array, aad: Uint8Array): NobleAead

internal external interface NobleAead {
    fun encrypt(plaintext: Uint8Array): Uint8Array

    /** Throws when the tag does not match. */
    fun decrypt(ciphertext: Uint8Array): Uint8Array
}
