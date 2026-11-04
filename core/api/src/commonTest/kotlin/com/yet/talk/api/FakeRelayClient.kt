package com.yet.talk.api

import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.message.Filter
import com.yet.talk.nostr.relay.PublishResult
import com.yet.talk.nostr.relay.RelayClient
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Returns [events] for every query and records what was asked. */
class FakeRelayClient(var events: List<Event> = emptyList(), var count: Long? = null) : RelayClient {
    val queries = mutableListOf<List<Filter>>()
    val published = mutableListOf<Pair<Event, Collection<String>>>()

    override suspend fun query(filters: List<Filter>, relays: Collection<String>, timeout: Duration): List<Event> {
        queries += filters
        return events
    }

    override fun subscribe(filters: List<Filter>, relays: Collection<String>): Flow<Event> = emptyFlow()

    override suspend fun publish(event: Event, relays: Collection<String>, timeout: Duration): PublishResult {
        published += event to relays
        return PublishResult(relays.toSet(), emptyMap(), emptySet())
    }

    override suspend fun count(filters: List<Filter>, relays: Collection<String>, timeout: Duration): Long? = count
}

object TestRelays : RelaySelection {
    override suspend fun readRelays() = listOf("wss://read")
    override suspend fun writeRelays() = listOf("wss://write")
}
