package com.yet.talk.data.session

import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.nostr.crypto.hmacSha256
import com.yet.talk.nostr.key.secureRandomHex
import com.yet.talk.nostr.nip.Nip44
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The key chats are encrypted with in the database: random, and stored sealed under a key derived from the
 * account key, so whatever locks the account key (the app lock) locks the chats too.
 */
@SingleIn(AppScope::class)
@Inject
@OptIn(ExperimentalStdlibApi::class)
internal class ChatKeyring(private val vault: KeyVault, private val session: Session) {
    private val mutex = Mutex()
    // The unsealed key, valid while the vault holds the same sealed copy (sign-out or erase clears it).
    private var cached: Pair<String, ByteArray>? = null

    suspend fun key(): ByteArray = mutex.withLock {
        val stored = vault.sealedChatKey()
        cached?.takeIf { it.first == stored }?.let { return it.second }
        val secret = session.secretKey() ?: throw DomainError.NotSignedIn
        val wrap = hmacSha256(secret.toHex().hexToByteArray(), LABEL.encodeToByteArray())
        val (sealed, key) = if (stored != null) {
            stored to Nip44.decrypt(stored, wrap)
        } else {
            val key = vault.takeLegacyChatKey() ?: secureRandomHex(32)
            Nip44.encrypt(key, wrap).also { vault.writeSealedChatKey(it) } to key
        }
        key.hexToByteArray().also { cached = sealed to it }
    }

    /** Erase chats: the key goes with them. */
    suspend fun erase() = mutex.withLock {
        vault.clearChatKey()
        cached = null
    }

    private companion object {
        const val LABEL = "talk-chat-storage-key-v1"
    }
}
