package com.yet.talk.data.sync

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.source.publish.PublishRemoteSource
import com.yet.talk.data.session.Session
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.model.PublishStates
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate
import dev.zacsweers.metro.Inject
import kotlin.time.Clock

/** Signs with the signed-in key, stores the event locally first, then publishes it. */
@Inject
class Publisher(
    private val session: Session,
    private val publishRemote: PublishRemoteSource,
    private val ingestor: EventIngestor,
    private val posts: PostDao,
    private val clock: Clock,
) {
    fun now(): Long = clock.now().epochSeconds

    suspend fun me(): String = session.requireSigner().publicKey()

    suspend fun sign(build: (createdAt: Long) -> EventTemplate): Event = session.requireSigner().sign(build(now()))

    /** Fails with [DomainError.NoRelayAccepted] when no relay confirms. */
    suspend fun publish(event: Event) {
        if (!publishRemote.publish(event).isAccepted) throw DomainError.NoRelayAccepted
    }

    /** Optimistic write: shown at once, then marked published or failed. */
    suspend fun storeAndPublish(event: Event) {
        val parsed = listOfNotNull(TalkEvent.parse(event))
        ingestor.ingest(parsed, PublishStates.PENDING)
        val result = runCatching { publish(event) }
        if (parsed.firstOrNull() is TalkEvent.Post) {
            posts.setPublishState(event.id, if (result.isSuccess) PublishStates.PUBLISHED else PublishStates.FAILED)
        }
        result.getOrThrow()
    }
}
