package com.yet.talk.nostr.crypto

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AesGcmTest {

    /** The GCM specification's test case 14: a zero 256-bit key and nonce over one zero block. */
    @Test
    fun matchesTheSpecificationVector() = runTest {
        val sealed = AesGcm.encrypt(ByteArray(16), ByteArray(32), ByteArray(12))
        assertEquals("cea7403d4d606b6e074ec5d3baf39d18" + "d0d1c8a799996bf0265b98b5d48ab919", sealed.toHexString())
    }

    @Test
    fun roundTripsAndRejectsATamperedFile() = runTest {
        val file = "a picture".encodeToByteArray()
        val sealed = AesGcm.encrypt(file)
        assertContentEquals(file, AesGcm.decrypt(sealed.ciphertext, sealed.key, sealed.nonce))
        val tampered = sealed.ciphertext.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }
        assertFails { AesGcm.decrypt(tampered, sealed.key, sealed.nonce) }
    }
}
