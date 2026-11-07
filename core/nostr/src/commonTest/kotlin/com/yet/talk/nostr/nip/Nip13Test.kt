package com.yet.talk.nostr.nip

import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.signer.LocalKeySigner
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Nip13Test {

    @Test
    fun countsLeadingZeroBits() {
        // NIP-13's example: 000000000e9d97a1... has 36 leading zero bits.
        assertEquals(36, Nip13.difficulty("000000000e9d97a1ab09fc381030b346cdd7a142ad57e6df0b46dc9bef6c7e2d"))
        assertEquals(0, Nip13.difficulty("f" + "0".repeat(63)))
        assertEquals(3, Nip13.difficulty("1" + "f".repeat(63)))
    }

    @Test
    fun aMinedEventKeepsItsTagsAndMeetsTheTarget() = runTest {
        val signer = LocalKeySigner(SecretKey.generate())
        val template = EventTemplate(20000, "hi", listOf(listOf("g", "u4pru"), listOf("n", "anon")), 1_000)
        val mined = Nip13.mine(template, signer.publicKey(), target = 12)
        val event = signer.sign(mined)
        assertTrue(Nip13.difficulty(event.id) >= 12)
        assertEquals(listOf("g", "n", "nonce"), event.tags.map { it[0] })
        assertEquals("12", event.tags.last()[2])
    }
}
