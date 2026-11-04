package com.yet.talk.data.sync

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.nostr.event.EventTemplate

/**
 * Publishes a new version of a replaceable list. Its `created_at` must be newer than the stored
 * one, or this device and relays would ignore it; two edits in one second would otherwise collide.
 */
internal suspend fun publishReplaceable(
    storedVersion: Long?,
    ingestor: EventIngestor,
    publisher: Publisher,
    build: (Long) -> EventTemplate,
) {
    val createdAt = maxOf(publisher.now(), (storedVersion ?: 0) + 1)
    val event = publisher.sign { build(createdAt) }
    ingestor.ingest(listOfNotNull(TalkEvent.parse(event)))
    publisher.publish(event)
}
