package com.yet.talk.api.source

import com.yet.talk.nostr.Event
import com.yet.talk.nostr.relay.PublishResult
import com.yet.talk.nostr.relay.RelayClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

fun interface PublishRemoteSource {
    /** Sends a signed event to the write relays. */
    suspend fun publish(event: Event): PublishResult
}

@ContributesBinding(AppScope::class)
@Inject
class RelayPublishRemoteSource(
    private val relays: RelayClient,
    private val selection: RelaySelection,
) : PublishRemoteSource {
    override suspend fun publish(event: Event): PublishResult = relays.publish(event, selection.writeRelays())
}
