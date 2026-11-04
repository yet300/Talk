package com.yet.talk.nostr

internal fun ByteArray.toHex(): String = toHexString()

/** Parses lowercase or uppercase hex; throws [IllegalArgumentException] on anything else. */
internal fun String.hexToBytes(): ByteArray = hexToByteArray()

internal fun String.isHex(byteLength: Int): Boolean =
    length == byteLength * 2 && all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
