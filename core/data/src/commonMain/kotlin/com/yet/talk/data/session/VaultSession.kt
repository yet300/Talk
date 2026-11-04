package com.yet.talk.data.session

import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.signer.LocalKeySigner
import com.yet.talk.nostr.signer.Signer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class VaultSession(private val vault: KeyVault) : Session {
    private val mutex = Mutex()
    private var loaded = false
    private val key = MutableStateFlow<SecretKey?>(null)

    override val viewer: Flow<String> = flow {
        load()
        emitAll(key.map { it?.publicKey.orEmpty() })
    }

    override suspend fun signer(): Signer? = secretKey()?.let(::LocalKeySigner)

    override suspend fun secretKey(): SecretKey? {
        load()
        return key.value
    }

    override suspend fun signIn(key: SecretKey) = mutex.withLock {
        vault.write(key.toHex())
        this.key.value = key
        loaded = true
    }

    override suspend fun signOut() = mutex.withLock {
        vault.clear()
        key.value = null
        loaded = true
    }

    private suspend fun load() = mutex.withLock {
        if (!loaded) {
            key.value = vault.read()?.let(SecretKey::fromHex)
            loaded = true
        }
    }
}
