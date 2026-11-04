package com.yet.talk.nostr.relay

/** Opens text sockets to relays. The seam that lets the relay pool run against a fake in tests. */
fun interface RelayTransport {
    suspend fun connect(url: String): RelaySocket
}
