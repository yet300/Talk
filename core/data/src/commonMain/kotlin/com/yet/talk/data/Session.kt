package com.yet.talk.data

import com.yet.talk.domain.model.DomainError
import com.yet.talk.nostr.LocalKeySigner
import com.yet.talk.nostr.SecretKey
import com.yet.talk.nostr.Signer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Encrypted storage for the one secret the app holds. */
interface KeyVault {
    suspend fun read(): String?

    suspend fun write(secretKeyHex: String)

    suspend fun clear()
}

@ContributesBinding(AppScope::class)
@Inject
class KSafeKeyVault(private val ksafe: Lazy<KSafe>) : KeyVault {
    // KSafe encrypts by default (Keystore / Keychain / WebCrypto); it is opened on first use.
    override suspend fun read(): String? = ksafe.value.get(KEY, "").ifEmpty { null }

    override suspend fun write(secretKeyHex: String) = ksafe.value.put(KEY, secretKeyHex)

    override suspend fun clear() = ksafe.value.delete(KEY)

    private companion object {
        const val KEY = "nostr_secret_key"
    }
}

/** The signed-in key, loaded from the vault on first use. Internal to the data layer. */
interface Session {
    /** The signed-in pubkey, or "" when signed out; the DAOs use "" for "no viewer". */
    val viewer: Flow<String>

    suspend fun signer(): Signer?

    suspend fun requireSigner(): Signer = signer() ?: throw DomainError.NotSignedIn

    suspend fun signIn(key: SecretKey)

    suspend fun signOut()

    suspend fun secretKey(): SecretKey?
}

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
