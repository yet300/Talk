package com.yet.talk.api.source.publish

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.relay.PublishResult

interface PublishRemoteSource {
    /** The relays signed events go to. */
    suspend fun writeRelays(): List<String>

    /** Sends a signed event to [relays] and reports each relay's answer. */
    suspend fun publish(event: Event, relays: Collection<String>): PublishResult

    /** Sends a signed event to the write relays. */
    suspend fun publish(event: Event): PublishResult = publish(event, writeRelays())
}
