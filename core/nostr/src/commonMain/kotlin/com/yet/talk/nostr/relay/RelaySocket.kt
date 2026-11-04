package com.yet.talk.nostr.relay

import io.ktor.websocket.close
import kotlinx.coroutines.flow.Flow

interface RelaySocket {
    /** Text frames from the relay; completes when the socket closes. Collect once. */
    val incoming: Flow<String>

    suspend fun send(text: String)

    suspend fun close()
}
