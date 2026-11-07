package com.yet.talk.nostr.relay

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.message.Filter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Reading from and writing to a set of relays. Implemented by [RelayPool]; faked in tests of its users. */
interface RelayClient {
    suspend fun query(filters: List<Filter>, relays: Collection<String>, timeout: Duration = 10.seconds): List<Event>

    fun subscribe(filters: List<Filter>, relays: Collection<String>): Flow<Event>

    suspend fun publish(event: Event, relays: Collection<String>, timeout: Duration = 10.seconds): PublishResult

    suspend fun count(filters: List<Filter>, relays: Collection<String>, timeout: Duration = 5.seconds): Long?

    /** Every relay this client has talked to, and whether it is connected now. */
    val statuses: Flow<Map<String, RelayStatus>> get() = flowOf(emptyMap())

    /** The device is online again: relays waiting out a backoff try at once. */
    fun reconnectNow() = Unit
}

/** A relay connection's state, as Settings and the offline indicator show it. */
enum class RelayStatus { Idle, Connecting, Connected, Disconnected }
