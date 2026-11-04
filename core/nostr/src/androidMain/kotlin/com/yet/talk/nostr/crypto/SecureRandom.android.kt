package com.yet.talk.nostr.crypto

import java.security.SecureRandom

private val secureRandom = SecureRandom()

internal actual fun secureRandomBytes(size: Int): ByteArray =
    ByteArray(size).also(secureRandom::nextBytes)
