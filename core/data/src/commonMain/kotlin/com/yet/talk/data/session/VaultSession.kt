package com.yet.talk.data.session

import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.nip.Nip49
import com.yet.talk.nostr.signer.LocalKeySigner
import com.yet.talk.nostr.signer.Signer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The key from the vault. With the app lock on, the vault holds only an `ncryptsec`: until [unlock], there is no
 * key in memory and no viewer, so nothing syncs or signs.
 */
@SingleIn(AppScope::class)
@Inject
internal class VaultSession(private val vault: KeyVault) : Session {
    private val mutex = Mutex()
    private var loaded = false
    private val key = MutableStateFlow<SecretKey?>(null)
    private val lockState = MutableStateFlow(Session.LockState())

    override val viewer: Flow<String> = flow {
        load()
        emitAll(key.map { it?.publicKey.orEmpty() })
    }

    override val lock: Flow<Session.LockState> = flow {
        load()
        emitAll(lockState)
    }

    override suspend fun signer(): Signer? = secretKey()?.let(::LocalKeySigner)

    override suspend fun secretKey(): SecretKey? {
        load()
        return key.value
    }

    override suspend fun signIn(key: SecretKey) = mutex.withLock {
        vault.write(key.toHex())
        this.key.value = key
        lockState.value = Session.LockState()
        loaded = true
    }

    override suspend fun signOut() = mutex.withLock {
        vault.clear()
        key.value = null
        lockState.value = Session.LockState()
        loaded = true
    }

    override suspend fun unlock(passcode: String): Boolean {
        load()
        return mutex.withLock {
            val sealed = vault.readLocked() ?: return@withLock key.value != null
            val hex = Nip49.decrypt(sealed, passcode) ?: return@withLock false
            key.value = SecretKey.fromHex(hex) ?: return@withLock false
            lockState.value = Session.LockState(enabled = true, locked = false)
            true
        }
    }

    override suspend fun setPasscode(passcode: String) {
        load()
        mutex.withLock {
            val current = key.value ?: throw DomainError.NotSignedIn
            vault.writeLocked(Nip49.encrypt(current.toHex(), passcode))
            lockState.value = Session.LockState(enabled = true, locked = false)
        }
    }

    override suspend fun removePasscode() {
        load()
        mutex.withLock {
            val current = key.value ?: throw DomainError.NotSignedIn
            vault.write(current.toHex())
            lockState.value = Session.LockState()
        }
    }

    private suspend fun load() = mutex.withLock {
        if (!loaded) {
            val plain = vault.read()?.let(SecretKey::fromHex)
            key.value = plain
            lockState.value = if (plain == null && vault.readLocked() != null) Session.LockState(enabled = true, locked = true) else Session.LockState()
            loaded = true
        }
    }
}
