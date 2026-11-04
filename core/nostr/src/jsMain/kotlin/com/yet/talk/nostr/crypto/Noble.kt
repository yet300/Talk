@file:JsModule("@noble/curves/secp256k1.js")
@file:JsNonModule

package com.yet.talk.nostr.crypto

import org.khronos.webgl.Uint8Array

internal external val schnorr: NobleSchnorr

internal external interface NobleSchnorr {
    fun getPublicKey(secretKey: Uint8Array): Uint8Array

    fun sign(message: Uint8Array, secretKey: Uint8Array, auxRand: Uint8Array): Uint8Array

    fun verify(signature: Uint8Array, message: Uint8Array, publicKey: Uint8Array): Boolean
}
