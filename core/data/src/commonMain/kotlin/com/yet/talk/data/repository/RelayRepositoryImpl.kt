package com.yet.talk.data.repository

import com.yet.talk.api.source.relay.DefaultRelays
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.publishReplaceable
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.RelayListDao
import com.yet.talk.domain.model.relay.Relay
import com.yet.talk.domain.repository.RelayRepository
import com.yet.talk.nostr.nip.RelayListEntry
import com.yet.talk.nostr.nip.relayList
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
internal class RelayRepositoryImpl(
    private val relayLists: RelayListDao,
    private val ingestor: EventIngestor,
    private val session: Session,
    private val publisher: Publisher,
) : RelayRepository {

    override val relays: Flow<List<Relay>> = session.viewer.flatMapLatest { viewer ->
        val defaults = DefaultRelays.urls.map { Relay(it) }
        if (viewer.isEmpty()) {
            flowOf(defaults)
        } else {
            relayLists.observe(viewer).map { stored -> stored.map { Relay(it.url, it.read, it.write) }.ifEmpty { defaults } }
        }
    }

    override suspend fun setRelays(relays: List<Relay>): Result<Unit> = remote {
        val me = publisher.me()
        val entries = relays.map { RelayListEntry(it.url, it.read, it.write) }
        publishReplaceable(relayLists.version(me), ingestor, publisher) { relayList(entries, it) }
    }
}
