package com.yet.talk.api.source.channel

import com.yet.talk.nostr.event.Event
import kotlinx.coroutines.flow.Flow

/** Location channel events on the relays near an area. */
interface ChannelRemoteSource {
    /** Kinds 20000 and 20001 for [geohashes] since [since], then live, from [relays]; verified, not stored. */
    fun events(geohashes: Collection<String>, since: Long, relays: Collection<String>, presenceOnly: Boolean = false): Flow<Event>

    /** True when at least one relay accepts it. */
    suspend fun publish(event: Event, relays: Collection<String>): Boolean
}
