package com.yet.talk.data

import com.yet.talk.data.fake.TestData
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.repository.Feed
import com.yet.talk.nostr.encoding.Nip19
import com.yet.talk.nostr.key.SecretKey
import androidx.paging.testing.asSnapshot
import com.yet.talk.domain.model.network.RelayState
import com.yet.talk.nostr.relay.RelayStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Publishing again goes only to the relays that refused for a passing reason. */
class OutboxTest {

    private fun outboxTest(body: suspend TestScope.(TestData) -> Unit) = runTest {
        val factory = testDriverFactory() ?: return@runTest
        body(TestData(factory, backgroundScope, StandardTestDispatcher(testScheduler)))
    }

    @Test
    fun onlyTheRelaysThatRefusedForNowAreAskedAgain() = outboxTest { data ->
        data.network.writeRelays = listOf("wss://ok", "wss://busy", "wss://silent", "wss://banned")
        data.network.refusing += mapOf("wss://busy" to "rate-limited: slow down", "wss://silent" to "", "wss://banned" to "blocked: no")
        data.accounts.importKey(Nip19.nsec(SecretKey.generate())).getOrThrow()

        val id = data.posts.publish("hello", emptyList(), null).getOrThrow()
        assertEquals(setOf("wss://ok", "wss://busy", "wss://silent", "wss://banned"), data.network.publishedTo.last().second)

        // The busy and the silent relay are owed it; the blocked one is not asked again.
        data.network.refusing.clear()
        data.clock.seconds += 31
        advanceTimeBy(31_000)
        runCurrent()
        val retry = data.network.publishedTo.last()
        assertEquals(id.value to setOf("wss://busy", "wss://silent"), retry)
    }

    @Test
    fun retryingAPostThatNoRelayTookGoesOnlyToTheOwedRelays() = outboxTest { data ->
        data.network.writeRelays = listOf("wss://a", "wss://b")
        data.network.refusing += mapOf("wss://a" to "", "wss://b" to "error: down")
        data.accounts.importKey(Nip19.nsec(SecretKey.generate())).getOrThrow()
        data.posts.publish("hello", emptyList(), null)
        val post = data.posts.feed(Feed.Latest).asSnapshot().first()
        assertEquals(PublishState.Failed, post.publishState)

        data.network.refusing.remove("wss://b")
        data.posts.retryPublish(post.id).getOrThrow()
        assertEquals(setOf("wss://a", "wss://b"), data.network.publishedTo.last().second)
        // Now only a is owed; b took it.
        data.network.refusing.clear()
        data.posts.retryPublish(post.id).getOrThrow()
        assertEquals(setOf("wss://a"), data.network.publishedTo.last().second)
    }
}

class NetworkStatusTest {

    private fun networkTest(body: suspend TestScope.(TestData) -> Unit) = runTest {
        val factory = testDriverFactory() ?: return@runTest
        body(TestData(factory, backgroundScope, StandardTestDispatcher(testScheduler)))
    }

    @Test
    fun offlineWhenTheDeviceIsOrNoRelayCanBeReached() = networkTest { data ->
        data.accounts.importKey(Nip19.nsec(SecretKey.generate())).getOrThrow()
        data.relays.setRelays(listOf(com.yet.talk.domain.model.relay.Relay("wss://a"), com.yet.talk.domain.model.relay.Relay("wss://b")))
        data.network.relayStatuses.value = mapOf("wss://a" to RelayStatus.Disconnected, "wss://b" to RelayStatus.Connected)
        var status = data.networkStatus.status.first { it.relays.size == 2 }
        assertEquals(mapOf("wss://a" to RelayState.Unreachable, "wss://b" to RelayState.Connected), status.relays)
        assertEquals(false, status.isOffline)

        data.network.relayStatuses.value = mapOf("wss://a" to RelayStatus.Disconnected, "wss://b" to RelayStatus.Disconnected)
        status = data.networkStatus.status.first { it.relays.values.all { s -> s == RelayState.Unreachable } }
        assertEquals(true, status.isOffline)

        data.network.relayStatuses.value = mapOf("wss://b" to RelayStatus.Connected)
        data.network.online.value = false
        assertEquals(true, data.networkStatus.status.first { !it.deviceOnline }.isOffline)

        // Back online: relays try again at once.
        data.network.online.value = true
        data.networkStatus.status.first { it.deviceOnline }
        testScheduler.runCurrent()
        assertEquals(1, data.network.reconnects)
    }
}
