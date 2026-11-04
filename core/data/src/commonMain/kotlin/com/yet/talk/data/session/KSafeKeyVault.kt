package com.yet.talk.data.session

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import eu.anifantakis.lib.ksafe.KSafe

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
