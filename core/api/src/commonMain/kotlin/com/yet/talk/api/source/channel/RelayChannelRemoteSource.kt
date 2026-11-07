package com.yet.talk.api.source.channel

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.message.Filter
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.relay.RelayClient
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
internal class RelayChannelRemoteSource(private val relays: RelayClient) : ChannelRemoteSource {

    override fun events(geohashes: Collection<String>, since: Long, relays: Collection<String>, presenceOnly: Boolean): Flow<Event> {
        val kinds = if (presenceOnly) listOf(Kinds.GEOHASH_PRESENCE) else listOf(Kinds.GEOHASH_MESSAGE, Kinds.GEOHASH_PRESENCE)
        return this.relays.subscribe(listOf(Filter(kinds = kinds, tags = mapOf('g' to geohashes.toList()), since = since, limit = LIMIT)), relays)
    }

    override suspend fun publish(event: Event, relays: Collection<String>): Boolean = this.relays.publish(event, relays).isAccepted

    private companion object {
        const val LIMIT = 200
    }
}
