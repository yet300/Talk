package com.yet.talk.nostr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Vectors from NIP-19. */
class Nip19Test {

    @Test
    fun encodesAndDecodesNpub() {
        val hex = "7e7e9c42a91bfef19fa929e5fda1b72e0ebc1a4c1141673e2794234d86addf4e"
        val npub = "npub10elfcs4fr0l0r8af98jlmgdh9c8tcxjvz9qkw038js35mp4dma8qzvjptg"
        assertEquals(npub, Nip19.npub(hex))
        assertEquals(Nip19.Entity.PublicKey(hex), Nip19.decode(npub))
        assertEquals("npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6", Nip19.npub("3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"))
    }

    @Test
    fun encodesAndDecodesNsec() {
        val hex = "67dea2ed018072d675f5415ecfaed7d2597555e202d85b3d65ea4e58d2d92ffa"
        val nsec = "nsec1vl029mgpspedva04g90vltkh6fvh240zqtv9k0t9af8935ke9laqsnlfe5"
        val key = SecretKey.fromHex(hex)!!
        assertEquals(nsec, Nip19.nsec(key))
        assertEquals(hex, assertIs<Nip19.Entity.PrivateKey>(Nip19.decode(nsec)).secretKey.toHex())
    }

    @Test
    fun roundTripsNotesAndAcceptsNostrUri() {
        val id = "d9be1ee0493d71fd4959a5fd103049aeb44d91ec571c73ceac0fb85814312b81"
        assertEquals(Nip19.Entity.Note(id), Nip19.decode("nostr:" + Nip19.note(id)))
    }

    @Test
    fun rejectsMalformedInput() {
        assertNull(Nip19.decode("npub10elfcs4fr0l0r8af98jlmgdh9c8tcxjvz9qkw038js35mp4dma8qzvjptq")) // bad checksum
        assertNull(Nip19.decode("NPUB10elfcs4fr0l0r8af98jlmgdh9c8tcxjvz9qkw038js35mp4dma8qzvjptg")) // mixed case
        assertNull(Nip19.decode("nprofile1qqsrhuxx8l9ex335q7he0f09aej04zpazpl0ne2cgukyawd24mayt8gpp4mhxue69uhhytnc9e3k7mgpz4mhxue69uhkg6nzv9ejuumpv34kytnrdaksjlyr9p"))
        assertNull(Nip19.decode("hello"))
    }
}
