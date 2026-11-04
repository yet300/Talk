package com.yet.talk.nostr.event

import com.yet.talk.nostr.encoding.toHex
import org.kotlincrypto.hash.sha2.SHA256

/**
 * NIP-01 event id: sha256 of `[0,pubkey,created_at,kind,tags,content]` serialised without whitespace.
 * Strings are escaped the way `JSON.stringify` does (what nostr-tools and most relays use), so ids
 * match other clients byte for byte; kotlinx-serialization is not used because its escaping differs.
 */
internal object EventHash {

    fun id(pubkey: String, createdAt: Long, kind: Int, tags: List<List<String>>, content: String): String =
        SHA256().digest(serialize(pubkey, createdAt, kind, tags, content).encodeToByteArray()).toHex()

    fun serialize(pubkey: String, createdAt: Long, kind: Int, tags: List<List<String>>, content: String): String =
        buildString {
            append("[0,")
            appendJsonString(pubkey)
            append(',').append(createdAt).append(',').append(kind).append(",[")
            tags.forEachIndexed { tagIndex, tag ->
                if (tagIndex > 0) append(',')
                append('[')
                tag.forEachIndexed { valueIndex, value ->
                    if (valueIndex > 0) append(',')
                    appendJsonString(value)
                }
                append(']')
            }
            append("],")
            appendJsonString(content)
            append(']')
        }

    private fun StringBuilder.appendJsonString(value: String) {
        append('"')
        for (char in value) {
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                else -> if (char < ' ') {
                    append("\\u00")
                    append(HEX_DIGITS[char.code shr 4]).append(HEX_DIGITS[char.code and 0xF])
                } else {
                    append(char)
                }
            }
        }
        append('"')
    }

    private const val HEX_DIGITS = "0123456789abcdef"
}
