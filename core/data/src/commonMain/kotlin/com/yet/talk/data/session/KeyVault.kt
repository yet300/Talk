package com.yet.talk.data.session

/** Encrypted storage for the one secret the app holds. */
interface KeyVault {
    suspend fun read(): String?

    suspend fun write(secretKeyHex: String)

    suspend fun clear()
}
