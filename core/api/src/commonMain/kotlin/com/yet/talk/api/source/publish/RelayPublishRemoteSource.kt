package com.yet.talk.api.source.publish

import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.relay.PublishResult
import com.yet.talk.nostr.relay.RelayClient
import dev.zacsweers.metro.Inject

@Inject
internal class RelayPublishRemoteSource(
    private val relays: RelayClient,
    private val selection: RelaySelection,
) : PublishRemoteSource {
    override suspend fun writeRelays(): List<String> = selection.writeRelays()

    override suspend fun publish(event: Event, relays: Collection<String>): PublishResult = this.relays.publish(event, relays)
}
