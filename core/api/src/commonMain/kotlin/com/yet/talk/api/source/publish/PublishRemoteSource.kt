package com.yet.talk.api.source.publish

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.relay.PublishResult

fun interface PublishRemoteSource {
    /** Sends a signed event to the write relays. */
    suspend fun publish(event: Event): PublishResult
}
