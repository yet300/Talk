package com.yet.talk.data.sync

import com.yet.talk.api.source.publish.PublishRemoteSource
import com.yet.talk.database.dao.OutboxDao
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.model.PublishStates
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.relay.PublishResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Publishing with memory: a relay that refuses for a passing reason (rate-limited, an error, no answer, AUTH first)
 * stays owed the event and gets it again later, with backoff; relays that took it, already had it, or refused it
 * for good (blocked, invalid, restricted, pow) are not asked again. A retry goes only to the relays still owed.
 */
@SingleIn(AppScope::class)
@Inject
internal class Outbox(
    private val remote: PublishRemoteSource,
    private val outbox: OutboxDao,
    private val posts: PostDao,
    private val clock: Clock,
    scope: CoroutineScope,
) {
    private val wakeUps = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            while (true) {
                runCatching { retryDue() }
                withTimeoutOrNull(CHECK_EVERY) { wakeUps.receive() }
            }
        }
    }

    /** Back online, or a relay reconnected: retry what is owed now rather than at its time. */
    fun wake() {
        wakeUps.trySend(Unit)
    }

    /**
     * Sends [event] to [relays], or to the relays still owed it (a retry), or to the write relays. The answer
     * counts a relay that already had it as accepted.
     */
    suspend fun publish(event: Event, relays: Collection<String>? = null): PublishResult {
        val targets = relays ?: outbox.relaysFor(event.id).keys.ifEmpty { remote.writeRelays() }
        return send(event, targets, attempts = outbox.relaysFor(event.id))
    }

    private suspend fun send(event: Event, targets: Collection<String>, attempts: Map<String, Long>): PublishResult {
        val result = remote.publish(event, targets)
        val duplicates = result.rejected.filterValues { it.prefix() == "duplicate" }.keys
        val took = result.accepted + duplicates
        val retry = result.noAnswer + result.rejected.filter { (relay, reason) -> relay !in duplicates && reason.prefix() in PASSING }.keys
        val json = event.toJson()
        targets.forEach { relay ->
            if (relay in retry) {
                val tries = (attempts[relay] ?: 0) + 1
                if (tries > MAX_ATTEMPTS) outbox.settle(event.id, relay) else outbox.owe(event.id, relay, json, tries, now() + backoff(tries))
            } else {
                outbox.settle(event.id, relay)
            }
        }
        return PublishResult(took, result.rejected - duplicates, result.noAnswer)
    }

    private suspend fun retryDue() {
        outbox.due(now()).groupBy { it.eventId }.forEach { (_, entries) ->
            val event = Event.fromJson(entries.first().eventJson)
            val result = runCatching { send(event, entries.map { it.relay }, entries.associate { it.relay to it.attempts }) }.getOrNull()
            // A post shown as Not sent is sent once any relay takes it.
            if (result?.isAccepted == true) posts.setPublishState(event.id, PublishStates.PUBLISHED)
        }
    }

    /** 30 s, 1 min, 2 min… up to an hour. */
    private fun backoff(attempt: Long): Long = min(BASE_SECONDS shl (attempt - 1).toInt().coerceIn(0, 7), MAX_SECONDS)

    private fun String.prefix() = substringBefore(':').trim().lowercase()

    private fun now() = clock.now().epochSeconds

    private companion object {
        /** NIP-01's machine-readable prefixes that may succeed later; "" is a relay that gave no reason. */
        val PASSING = setOf("rate-limited", "error", "auth-required", "")
        const val MAX_ATTEMPTS = 10
        const val BASE_SECONDS = 30L
        const val MAX_SECONDS = 3_600L
        val CHECK_EVERY = 30.seconds
    }
}
