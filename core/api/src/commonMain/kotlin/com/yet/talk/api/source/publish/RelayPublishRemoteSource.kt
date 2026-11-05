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
    override suspend fun publish(event: Event): PublishResult = relays.publish(event, selection.writeRelays())
}
