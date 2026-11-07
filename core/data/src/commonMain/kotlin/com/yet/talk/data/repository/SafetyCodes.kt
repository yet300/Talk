package com.yet.talk.data.repository

import com.yet.talk.domain.model.chat.SafetyCode
import com.yet.talk.nostr.crypto.Sha256

/**
 * Safety codes: SHA-256 over both public keys in a fixed order, shown as four groups of five digits (about 66
 * bits), the same on both devices. The QR code carries the keys and the code, so a scan proves both.
 */
internal object SafetyCodes {
    private const val LABEL = "talk-safety-code-v1"

    @OptIn(ExperimentalStdlibApi::class)
    fun of(me: String, them: String): SafetyCode {
        val (a, b) = listOf(me, them).sorted()
        val digest = Sha256.hex("$LABEL:$a:$b".encodeToByteArray()).hexToByteArray()
        val groups = (0 until 4).map { i ->
            val value = (0 until 5).fold(0L) { acc, j -> (acc shl 8) or (digest[i * 5 + j].toLong() and 0xff) }
            (value % 100_000).toString().padStart(5, '0')
        }
        return SafetyCode(groups, "talk-verify:1:$a:$b:${groups.joinToString("")}")
    }
}
