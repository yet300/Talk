package com.yet.talk.data.session

import com.yet.talk.nostr.key.secureRandomHex
import dev.zacsweers.metro.Inject
import eu.anifantakis.lib.ksafe.KSafe

@Inject
internal class KSafeKeyVault(private val ksafe: Lazy<KSafe>) : KeyVault {
    // KSafe encrypts by default (Keystore / Keychain / WebCrypto); it is opened on first use.
    override suspend fun read(): String? = ksafe.value.get(KEY, "").ifEmpty { null }

    override suspend fun write(secretKeyHex: String) = ksafe.value.put(KEY, secretKeyHex)

    override suspend fun clear() {
        ksafe.value.delete(KEY)
        ksafe.value.delete(CHAT_KEY)
    }

    override suspend fun chatKey(): String =
        ksafe.value.get(CHAT_KEY, "").ifEmpty { secureRandomHex(32).also { ksafe.value.put(CHAT_KEY, it) } }

    private companion object {
        const val KEY = "nostr_secret_key"
        const val CHAT_KEY = "chat_storage_key"
    }
}
