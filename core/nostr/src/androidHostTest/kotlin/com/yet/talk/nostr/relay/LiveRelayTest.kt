package com.yet.talk.nostr.relay

import com.yet.talk.nostr.EventVerifier
import com.yet.talk.nostr.Filter
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Reads from real public relays over the Ktor transport. Opt-in, read-only:
 * `TALK_LIVE_RELAYS=1 ./gradlew :core:nostr:testAndroidHostTest --tests '*LiveRelayTest*'`
 */
class LiveRelayTest {

    @Test
    fun queriesRealRelays() = runBlocking {
        assumeTrue(System.getenv("TALK_LIVE_RELAYS") == "1")
        val client = HttpClient(OkHttp) { install(WebSockets) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val pool = RelayPool(KtorRelayTransport(client), scope)
            val events = pool.query(listOf(Filter(kinds = listOf(1), limit = 10)), listOf("wss://nos.lol", "wss://relay.primal.net"))
            println("LiveRelayTest: ${events.size} events")
            assertTrue(events.isNotEmpty())
            assertTrue(events.all(EventVerifier::verify))
        } finally {
            scope.cancel()
            client.close()
        }
    }
}
