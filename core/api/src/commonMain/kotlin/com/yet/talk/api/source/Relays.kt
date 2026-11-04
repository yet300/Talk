package com.yet.talk.api.source

/** Where to read from and write to. Implemented by the data layer from the user's NIP-65 list. */
interface RelaySelection {
    suspend fun readRelays(): List<String>

    suspend fun writeRelays(): List<String>
}

/** Used until the user has a relay list of their own. All support NIP-01 tag filters such as `#L`. */
object DefaultRelays {
    val urls = listOf("wss://nos.lol", "wss://relay.primal.net", "wss://relay.damus.io")
}

/** Relays cap filter sizes; ids and authors are sent in chunks of this size. */
internal const val FILTER_CHUNK = 100
