package com.yet.talk.data.session

/**
 * Encrypted storage for the secrets the app holds: the account key, either plain (KSafe still encrypts it) or, with
 * the web's app lock, as a NIP-49 `ncryptsec`; and the chat storage key, sealed under the account key.
 */
internal interface KeyVault {
    /** The account key (hex) when there is no app lock. */
    suspend fun read(): String?

    /** Stores the account key and turns the app lock off. */
    suspend fun write(secretKeyHex: String)

    /** The account key as an `ncryptsec`, when the app lock is on. */
    suspend fun readLocked(): String?

    /** Stores the account key only as an `ncryptsec` (the app lock is on). */
    suspend fun writeLocked(ncryptsec: String)

    /** Removes every secret: account key, lock, chat key. */
    suspend fun clear()

    /** The chat storage key, NIP-44-sealed under a key derived from the account key; null before first use. */
    suspend fun sealedChatKey(): String?

    suspend fun writeSealedChatKey(sealed: String)

    /** A chat key stored unsealed by an earlier version; it is removed once read. */
    suspend fun takeLegacyChatKey(): String?

    suspend fun clearChatKey()
}
