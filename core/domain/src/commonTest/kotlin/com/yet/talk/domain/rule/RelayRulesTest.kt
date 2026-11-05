package com.yet.talk.domain.rule

import com.yet.talk.domain.model.error.DomainError
import kotlin.test.Test
import kotlin.test.assertEquals

class RelayRulesTest {

    @Test
    fun normalizesSchemeHostAndTrailingSlash() {
        assertEquals("wss://relay.example.com", RelayRules.normalize("  WSS://Relay.Example.com/ ").getOrThrow())
        assertEquals("wss://relay.example.com/path", RelayRules.normalize("wss://relay.example.com/path").getOrThrow())
        assertEquals("ws://localhost:7777", RelayRules.normalize("ws://localhost:7777").getOrThrow())
    }

    @Test
    fun rejectsAnythingButSecureWebSockets() {
        listOf("https://relay.example.com", "relay.example.com", "ws://relay.example.com", "wss://", "wss://a b").forEach {
            assertEquals(DomainError.InvalidRelayUrl, RelayRules.normalize(it).exceptionOrNull(), it)
        }
    }
}
