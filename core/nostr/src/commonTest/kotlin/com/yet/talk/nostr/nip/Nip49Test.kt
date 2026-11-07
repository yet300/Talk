package com.yet.talk.nostr.nip

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

/** NIP-49 runs only where Talk uses it (the web), so these tests skip elsewhere. */
class Nip49Test {

    @Test
    fun decryptsTheSpecificationVector() = runTest(timeout = 2.minutes) {
        if (!Nip49.isSupported) return@runTest
        val ncryptsec = "ncryptsec1qgg9947rlpvqu76pj5ecreduf9jxhselq2nae2kghhvd5g7dgjtcxfqtd67p9m0w57lspw8gsq6yphnm8623nsl8xn9j4jdzz84zm3frztj3z7s35vpzmqf6ksu8r89qk5z2zxfmu5gv8th8wclt0h4p"
        assertEquals("3501454135014541350145413501453fefb02227e449e57cf4d3a3ce05378683", Nip49.decrypt(ncryptsec, "nostr"))
        assertNull(Nip49.decrypt(ncryptsec, "wrong"))
    }

    @Test
    fun roundTripsAndNormalisesThePassword() = runTest(timeout = 2.minutes) {
        if (!Nip49.isSupported) return@runTest
        val secret = "3501454135014541350145413501453fefb02227e449e57cf4d3a3ce05378683"
        // A cheap work factor for the test; Talk uses 16.
        val sealed = Nip49.encrypt(secret, "ÅΩẛ̣", logN = 8)
        assertEquals(secret, Nip49.decrypt(sealed, "ÅΩẛ̣"))
    }
}
