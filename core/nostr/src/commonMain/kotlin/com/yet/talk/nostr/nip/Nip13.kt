package com.yet.talk.nostr.nip

import com.yet.talk.nostr.event.EventHash
import com.yet.talk.nostr.event.EventTemplate
import kotlin.random.Random

/**
 * NIP-13 proof of work: a `["nonce", <n>, <target>]` tag is varied until the event id starts with [target] zero
 * bits. Location channels mine their messages, as bitchat does, so relays and clients that filter spam by work
 * accept them.
 */
object Nip13 {
    /** The tag's place among the others is kept; an existing nonce tag is replaced. */
    fun mine(template: EventTemplate, pubkey: String, target: Int, maxIterations: Int = 4_000_000): EventTemplate {
        val base = template.tags.filterNot { it.firstOrNull() == "nonce" }
        var nonce = Random.nextLong(0, 1_000_000)
        repeat(maxIterations) {
            val tags = base + listOf(listOf("nonce", nonce.toString(), target.toString()))
            val id = EventHash.id(pubkey, template.createdAt, template.kind, tags, template.content)
            if (difficulty(id) >= target) return template.copy(tags = tags)
            nonce++
        }
        error("No nonce reached $target bits")
    }

    /** Leading zero bits of a hex event id. */
    fun difficulty(id: String): Int {
        var bits = 0
        for (c in id) {
            val nibble = c.digitToInt(16)
            if (nibble == 0) {
                bits += 4
            } else {
                bits += nibble.countLeadingZeroBits() - 28
                break
            }
        }
        return bits
    }
}
