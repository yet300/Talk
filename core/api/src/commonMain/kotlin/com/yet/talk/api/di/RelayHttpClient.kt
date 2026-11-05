package com.yet.talk.api.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.websocket.WebSockets
import kotlin.time.Duration.Companion.seconds

/**
 * The HTTP client for relays and Blossom, given the platform's engine. Pings keep idle relay sockets
 * alive and detect half-open ones (network switch, NAT timeout), so the relay pool reconnects.
 */
fun relayHttpClient(engine: HttpClientEngineFactory<*>): HttpClient = HttpClient(engine) {
    install(WebSockets) {
        pingIntervalMillis = PING_INTERVAL.inWholeMilliseconds
    }
}

private val PING_INTERVAL = 20.seconds
