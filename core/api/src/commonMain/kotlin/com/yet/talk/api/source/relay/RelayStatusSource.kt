package com.yet.talk.api.source.relay

import com.yet.talk.nostr.relay.RelayClient
import com.yet.talk.nostr.relay.RelayStatus
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

/** The relays' connection states, for Settings and the offline indicator. */
fun interface RelayStatusSource {
    fun statuses(): Flow<Map<String, RelayStatus>>
}

@Inject
internal class RelayClientStatusSource(private val relays: RelayClient) : RelayStatusSource {
    override fun statuses(): Flow<Map<String, RelayStatus>> = relays.statuses
}
