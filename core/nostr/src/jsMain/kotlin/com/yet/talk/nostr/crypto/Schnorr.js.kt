package com.yet.talk.nostr.crypto

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array

internal actual fun platformIsValidSecretKey(secretKey: ByteArray): Boolean =
    runCatching { schnorr.getPublicKey(secretKey.toUint8Array()) }.isSuccess

internal actual fun platformPublicKey(secretKey: ByteArray): ByteArray =
    schnorr.getPublicKey(secretKey.toUint8Array()).toByteArray()

internal actual fun platformSign(message: ByteArray, secretKey: ByteArray, auxRand: ByteArray): ByteArray =
    schnorr.sign(message.toUint8Array(), secretKey.toUint8Array(), auxRand.toUint8Array()).toByteArray()

internal actual fun platformVerify(signature: ByteArray, message: ByteArray, publicKey: ByteArray): Boolean =
    schnorr.verify(signature.toUint8Array(), message.toUint8Array(), publicKey.toUint8Array())

internal actual fun platformSharedX(secretKey: ByteArray, publicKey: ByteArray): ByteArray =
    secp256k1.getSharedSecret(secretKey.toUint8Array(), (byteArrayOf(2) + publicKey).toUint8Array(), true).toByteArray().copyOfRange(1, 33)

internal actual fun secureRandomBytes(size: Int): ByteArray {
    val bytes = Uint8Array(size)
    js("globalThis.crypto").getRandomValues(bytes)
    return bytes.toByteArray()
}

// Kotlin/JS ByteArray is an Int8Array: reinterpret the same memory, then copy so nothing is shared.
internal fun ByteArray.toUint8Array(): Uint8Array {
    val view = unsafeCast<Int8Array>()
    return Uint8Array(view.buffer, view.byteOffset, view.length).asDynamic().slice().unsafeCast<Uint8Array>()
}

internal fun Uint8Array.toByteArray(): ByteArray =
    Int8Array(buffer, byteOffset, length).unsafeCast<ByteArray>().copyOf()
