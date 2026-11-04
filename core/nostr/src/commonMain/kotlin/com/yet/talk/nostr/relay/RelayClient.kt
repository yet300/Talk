package com.yet.talk.nostr.relay

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.message.Filter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow

/** Reading from and writing to a set of relays. Implemented by [RelayPool]; faked in tests of its users. */
interface RelayClient {
    suspend fun query(filters: List<Filter>, relays: Collection<String>, timeout: Duration = 10.seconds): List<Event>

    fun subscribe(filters: List<Filter>, relays: Collection<String>): Flow<Event>

    suspend fun publish(event: Event, relays: Collection<String>, timeout: Duration = 10.seconds): PublishResult

    suspend fun count(filters: List<Filter>, relays: Collection<String>, timeout: Duration = 5.seconds): Long?
}
