package com.yet.talk.nostr.crypto

/**
 * BIP-340 Schnorr signatures over secp256k1. libsecp256k1 on Android and iOS, @noble/curves on JS.
 * All keys are raw bytes: 32-byte secret keys, 32-byte x-only public keys, 64-byte signatures.
 */
internal object Schnorr {

    fun isValidSecretKey(secretKey: ByteArray): Boolean =
        secretKey.size == 32 && platformIsValidSecretKey(secretKey)

    fun publicKey(secretKey: ByteArray): ByteArray = platformPublicKey(secretKey)

    fun sign(message: ByteArray, secretKey: ByteArray, auxRand: ByteArray): ByteArray =
        platformSign(message, secretKey, auxRand)

    /** False for a wrong signature as well as for malformed input (bad lengths, a key off the curve). */
    fun verify(signature: ByteArray, message: ByteArray, publicKey: ByteArray): Boolean =
        signature.size == 64 && publicKey.size == 32 &&
            runCatching { platformVerify(signature, message, publicKey) }.getOrDefault(false)
}

internal expect fun platformIsValidSecretKey(secretKey: ByteArray): Boolean

internal expect fun platformPublicKey(secretKey: ByteArray): ByteArray

internal expect fun platformSign(message: ByteArray, secretKey: ByteArray, auxRand: ByteArray): ByteArray

internal expect fun platformVerify(signature: ByteArray, message: ByteArray, publicKey: ByteArray): Boolean

/** Cryptographically secure random bytes from the platform. */
internal expect fun secureRandomBytes(size: Int): ByteArray
