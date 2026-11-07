package com.yet.talk.data.session

import dev.zacsweers.metro.Inject
import eu.anifantakis.lib.ksafe.KSafe

@Inject
internal class KSafeKeyVault(private val ksafe: Lazy<KSafe>) : KeyVault {
    // KSafe encrypts by default (Keystore / Keychain / WebCrypto); it is opened on first use.
    override suspend fun read(): String? = get(KEY)

    override suspend fun write(secretKeyHex: String) {
        ksafe.value.put(KEY, secretKeyHex)
        ksafe.value.delete(LOCKED_KEY)
    }

    override suspend fun readLocked(): String? = get(LOCKED_KEY)

    override suspend fun writeLocked(ncryptsec: String) {
        ksafe.value.put(LOCKED_KEY, ncryptsec)
        ksafe.value.delete(KEY)
    }

    override suspend fun clear() {
        listOf(KEY, LOCKED_KEY, CHAT_KEY, LEGACY_CHAT_KEY).forEach { ksafe.value.delete(it) }
    }

    override suspend fun sealedChatKey(): String? = get(CHAT_KEY)

    override suspend fun writeSealedChatKey(sealed: String) = ksafe.value.put(CHAT_KEY, sealed)

    override suspend fun takeLegacyChatKey(): String? = get(LEGACY_CHAT_KEY)?.also { ksafe.value.delete(LEGACY_CHAT_KEY) }

    override suspend fun clearChatKey() {
        ksafe.value.delete(CHAT_KEY)
        ksafe.value.delete(LEGACY_CHAT_KEY)
    }

    private suspend fun get(key: String): String? = ksafe.value.get(key, "").ifEmpty { null }

    private companion object {
        const val KEY = "nostr_secret_key"
        const val LOCKED_KEY = "nostr_secret_key_ncryptsec"
        const val CHAT_KEY = "chat_storage_key_sealed"
        const val LEGACY_CHAT_KEY = "chat_storage_key"
    }
}
