package com.yet.talk.nostr.key

import com.yet.talk.nostr.encoding.isHex

/** True for a 32-byte hex x-only key. Does not check that the point is on the curve. */
fun isPublicKeyHex(value: String): Boolean = value.isHex(32)
