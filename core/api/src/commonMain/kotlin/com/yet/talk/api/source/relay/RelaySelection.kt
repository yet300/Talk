package com.yet.talk.api.source.relay

/** Where to read from and write to. Implemented by the data layer from the user's NIP-65 list. */
interface RelaySelection {
    suspend fun readRelays(): List<String>

    suspend fun writeRelays(): List<String>
}
