package com.yet.talk.api.source.chat

import com.yet.talk.api.source.relay.FILTER_CHUNK
import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.message.Filter
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.values
import com.yet.talk.nostr.relay.RelayClient
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
internal class RelayChatRemoteSource(private val relays: RelayClient, private val selection: RelaySelection) : ChatRemoteSource {

    override suspend fun inboxRelays(pubkeys: Collection<String>): Map<String, List<String>> {
        if (pubkeys.isEmpty()) return emptyMap()
        val filters = pubkeys.distinct().chunked(FILTER_CHUNK).map { Filter(kinds = listOf(Kinds.DM_RELAYS), authors = it) }
        val where = (selection.readRelays() + ChatRemoteSource.INDEXERS).distinct()
        return relays.query(filters, where)
            .groupBy { it.pubkey }
            .mapValues { (_, lists) -> lists.maxBy { it.createdAt }.tags.values("relay").filter { it.startsWith("wss://") } }
            .filterValues { it.isNotEmpty() }
    }

    override suspend fun publish(event: Event, relays: Collection<String>): Boolean = this.relays.publish(event, relays).isAccepted

    override fun wraps(pubkey: String, since: Long?, relays: Collection<String>): Flow<Event> =
        this.relays.subscribe(listOf(Filter(kinds = listOf(Kinds.GIFT_WRAP), tags = mapOf('p' to listOf(pubkey)), since = since)), relays)

    override suspend fun muteList(pubkey: String): Event? =
        relays.query(listOf(Filter(kinds = listOf(Kinds.MUTE_LIST), authors = listOf(pubkey))), (selection.readRelays() + selection.writeRelays()).distinct())
            .filter { it.pubkey == pubkey }
            .maxByOrNull { it.createdAt }
}
