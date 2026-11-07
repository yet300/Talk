package com.yet.talk.data.session

/** Encrypted storage for the one secret the app holds. */
internal interface KeyVault {
    suspend fun read(): String?

    suspend fun write(secretKeyHex: String)

    suspend fun clear()

    /** The key chats are encrypted with in the database (64 hex), made on first use; [clear] removes it. */
    suspend fun chatKey(): String
}
