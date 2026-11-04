package com.yet.talk.nostr.relay

import com.yet.talk.nostr.Event
import com.yet.talk.nostr.EventTemplate
import com.yet.talk.nostr.Filter
import com.yet.talk.nostr.LocalKeySigner
import com.yet.talk.nostr.SecretKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RelayPoolTest {

    private val signer = LocalKeySigner(SecretKey.generate())
    private val filter = listOf(Filter(kinds = listOf(1)))

    private suspend fun note(content: String, createdAt: Long): Event =
        signer.sign(EventTemplate(kind = 1, content = content, createdAt = createdAt))

    private fun TestScope.pool(vararg relays: Pair<String, FakeRelay>) =
        RelayPool(FakeTransport(relays.toMap()), backgroundScope)

    @Test
    fun queryMergesRelaysDeduplicatesAndSortsNewestFirst() = runTest {
        val old = note("old", 1)
        val new = note("new", 2)
        val pool = pool("wss://a" to FakeRelay(listOf(old, new)), "wss://b" to FakeRelay(listOf(new)))

        val events = pool.query(filter, listOf("wss://a", "wss://b"))

        assertEquals(listOf(new, old), events)
        assertTrue(currentTime < 10_000, "query should finish on EOSE, not on the timeout")
    }

    @Test
    fun queryDropsEventsThatFailVerification() = runTest {
        val good = note("good", 1)
        val forged = note("real", 2).copy(content = "forged")
        val pool = pool("wss://a" to FakeRelay(listOf(good, forged)))

        assertEquals(listOf(good), pool.query(filter, listOf("wss://a")))
    }

    @Test
    fun queryReturnsPartialResultsWhenARelayIsUnreachable() = runTest {
        val event = note("x", 1)
        val pool = pool("wss://a" to FakeRelay(listOf(event)))

        assertEquals(listOf(event), pool.query(filter, listOf("wss://a", "wss://down")))
    }

    @Test
    fun publishReportsAcceptedRejectedAndSilentRelays() = runTest {
        val event = note("hello", 1)
        val pool = pool("wss://ok" to FakeRelay(), "wss://no" to FakeRelay(acceptEvents = false))

        val result = pool.publish(event, listOf("wss://ok", "wss://no", "wss://down"))

        assertEquals(setOf("wss://ok"), result.accepted)
        assertEquals(mapOf("wss://no" to "blocked: test"), result.rejected)
        assertEquals(setOf("wss://down"), result.noAnswer)
        assertTrue(result.isAccepted)
    }

    @Test
    fun countUsesTheHighestAnswerAndNullWithoutSupport() = runTest {
        val events = listOf(note("a", 1), note("b", 2))
        val pool = pool("wss://a" to FakeRelay(events), "wss://b" to FakeRelay(events.take(1)), "wss://c" to FakeRelay(supportsCount = false))

        assertEquals(2, pool.count(filter, listOf("wss://a", "wss://b")))
        assertNull(pool.count(filter, listOf("wss://c")))
    }

    @Test
    fun subscriptionSurvivesReconnectAndClosesOnCancel() = runTest {
        val relay = FakeRelay()
        val pool = pool("wss://a" to relay)
        val live = note("live", 3)

        val received = mutableListOf<Event>()
        val job = launch { pool.subscribe(filter, listOf("wss://a")).take(1).toList(received) }
        runCurrent() // connection work runs in backgroundScope, which advanceUntilIdle() skips

        relay.sockets.single().drop()
        advanceTimeBy(2_000) // backoff, reconnect, re-send the REQ
        runCurrent()
        assertEquals(2, relay.sockets.size)
        val resent = relay.received.last { it.startsWith("[\"REQ\"") }
        val subscriptionId = resent.substringAfter("[\"REQ\",\"").substringBefore('"')

        relay.sockets.last().push(subscriptionId, live)
        job.join()

        assertEquals(listOf(live), received)
        runCurrent()
        assertTrue(relay.received.last().startsWith("[\"CLOSE\""))
    }

    @Test
    fun liveSubscriptionDeliversStoredThenNewEvents() = runTest {
        val stored = note("stored", 1)
        val relay = FakeRelay(listOf(stored))
        val pool = pool("wss://a" to relay)

        assertEquals(stored, pool.subscribe(filter, listOf("wss://a")).first())
    }
}
