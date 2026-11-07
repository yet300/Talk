package com.yet.talk.data.session

import com.yet.talk.data.fake.FakeKeyVault
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.nip.Nip49
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/** The web's app lock (NIP-49); it exists only where [Nip49.isSupported]. */
class AppLockTest {

    @Test
    fun aLockedKeyIsOnlyStoredEncryptedAndOpensWithThePasscode() = runTest(timeout = 2.minutes) {
        if (!Nip49.isSupported) return@runTest
        val vault = FakeKeyVault()
        val key = SecretKey.generate()
        VaultSession(vault).apply {
            signIn(key)
            setPasscode("correct horse")
        }
        assertNull(vault.stored, "no plain key once locked")
        assertTrue(assertNotNull(vault.locked).startsWith("ncryptsec1"))

        // The next start: locked, no viewer, no signer.
        val next = VaultSession(vault)
        assertEquals(Session.LockState(enabled = true, locked = true), next.lock.first())
        assertEquals("", next.viewer.first())
        assertNull(next.signer())

        assertFalse(next.unlock("wrong horse"))
        assertTrue(next.unlock("correct horse"))
        assertEquals(key.publicKey, next.viewer.first())
        assertEquals(Session.LockState(enabled = true, locked = false), next.lock.first())

        next.removePasscode()
        assertEquals(key.toHex(), vault.stored)
        assertNull(vault.locked)
    }
}
