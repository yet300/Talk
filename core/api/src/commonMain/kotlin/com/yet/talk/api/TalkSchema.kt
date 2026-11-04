package com.yet.talk.api

import com.yet.talk.nostr.Event

/** Talk's own conventions on top of the NIPs (docs/nostr-protocol.md). */
object TalkSchema {
    /** NIP-32 label namespace on every Talk event: the community boundary. */
    const val NAMESPACE = "com.yet.talk"

    /** A post revision: a regular event holding the full new markdown. */
    const val KIND_REVISION = 1010

    const val REVISION_TAG = "revision"

    object Role {
        const val POST = "post"
        const val REPLY = "reply"
        const val REVISION = "revision"
        const val REACTION = "reaction"
        const val REPOST = "repost"
        const val DELETION = "deletion"
    }

    fun labels(role: String): List<List<String>> = listOf(listOf("L", NAMESPACE), listOf("l", role, NAMESPACE))

    fun isTalkEvent(event: Event): Boolean = event.tags.any { it.size >= 2 && it[0] == "L" && it[1] == NAMESPACE }
}
