package com.yet.talk.nostr.crypto

import fr.acinq.secp256k1.Secp256k1

internal actual fun platformIsValidSecretKey(secretKey: ByteArray): Boolean =
    runCatching { Secp256k1.secKeyVerify(secretKey) }.getOrDefault(false)

// pubkeyCreate returns the 65-byte uncompressed key (0x04 || x || y); BIP-340 keys are x only.
internal actual fun platformPublicKey(secretKey: ByteArray): ByteArray =
    Secp256k1.pubkeyCreate(secretKey).copyOfRange(1, 33)

internal actual fun platformSign(message: ByteArray, secretKey: ByteArray, auxRand: ByteArray): ByteArray =
    Secp256k1.signSchnorr(message, secretKey, auxRand)

internal actual fun platformVerify(signature: ByteArray, message: ByteArray, publicKey: ByteArray): Boolean =
    Secp256k1.verifySchnorr(signature, message, publicKey)

// libsecp256k1's ecdh hashes the point; NIP-44 needs the raw x, so the point is multiplied by the key instead.
internal actual fun platformSharedX(secretKey: ByteArray, publicKey: ByteArray): ByteArray =
    Secp256k1.pubKeyTweakMul(byteArrayOf(2) + publicKey, secretKey).copyOfRange(1, 33)
