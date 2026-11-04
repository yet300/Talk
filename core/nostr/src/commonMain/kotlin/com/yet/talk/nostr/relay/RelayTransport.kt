package com.yet.talk.nostr.relay

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

/** Opens text sockets to relays. The seam that lets the relay pool run against a fake in tests. */
fun interface RelayTransport {
    suspend fun connect(url: String): RelaySocket
}

interface RelaySocket {
    /** Text frames from the relay; completes when the socket closes. Collect once. */
    val incoming: Flow<String>

    suspend fun send(text: String)

    suspend fun close()
}

/** WebSocket transport over Ktor. The [client] must have the `WebSockets` plugin installed. */
class KtorRelayTransport(private val client: HttpClient) : RelayTransport {

    override suspend fun connect(url: String): RelaySocket {
        val session = client.webSocketSession(url)
        return object : RelaySocket {
            override val incoming: Flow<String> =
                session.incoming.consumeAsFlow().filterIsInstance<Frame.Text>().map { it.readText() }

            override suspend fun send(text: String) = session.send(Frame.Text(text))

            override suspend fun close() = session.close()
        }
    }
}
