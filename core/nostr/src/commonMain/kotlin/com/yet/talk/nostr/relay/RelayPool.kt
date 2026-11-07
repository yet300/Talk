package com.yet.talk.nostr.relay

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventVerifier
import com.yet.talk.nostr.message.ClientMessage
import com.yet.talk.nostr.message.Filter
import com.yet.talk.nostr.message.RelayMessage
import kotlin.random.Random
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Talks to many relays at once. Every incoming event is verified (id and signature) before it is
 * emitted, and events are de-duplicated by id across relays.
 */
internal class RelayPool(
    private val transport: RelayTransport,
    private val scope: CoroutineScope,
    private val verify: (Event) -> Boolean = EventVerifier::verify,
    /** Where events are verified (sha256 + Schnorr): never the caller's (often the main) thread. */
    private val verifyDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val authenticator: RelayAuthenticator? = null,
) : RelayClient {
    private val connections = MutableStateFlow<Map<String, RelayConnection>>(emptyMap())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val statuses: Flow<Map<String, RelayStatus>> = connections.flatMapLatest { all ->
        if (all.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(all.values.map { connection -> connection.state.map { connection.url to it.toStatus() } }) { it.toMap() }
        }
    }

    private fun RelayConnection.State.toStatus() = when (this) {
        RelayConnection.State.Idle -> RelayStatus.Idle
        RelayConnection.State.Connecting -> RelayStatus.Connecting
        RelayConnection.State.Connected -> RelayStatus.Connected
        RelayConnection.State.Disconnected -> RelayStatus.Disconnected
    }

    fun connection(url: String): RelayConnection {
        connections.value[url]?.let { return it }
        connections.update { if (url in it) it else it + (url to RelayConnection(url, transport, scope, authenticator = authenticator)) }
        return connections.value.getValue(url)
    }

    /**
     * Stored events matching [filters], newest first. Finishes when every relay has sent EOSE, closed
     * the subscription or turned out unreachable, or after [timeout] with whatever arrived until then.
     */
    override suspend fun query(filters: List<Filter>, relays: Collection<String>, timeout: Duration): List<Event> {
        if (relays.isEmpty()) return emptyList()
        val subscriptionId = newSubscriptionId()
        val events = LinkedHashMap<String, Event>()
        withTimeoutOrNull(timeout) {
            merge(*relays.map { stream(it, subscriptionId, filters, untilEose = true) }.toTypedArray())
                .collect { events.getOrPut(it.id) { it } }
        }
        relays.forEach { connection(it).send(ClientMessage.Close(subscriptionId)) }
        return events.values.sortedByDescending { it.createdAt }
    }

    /** Stored and then live events matching [filters], de-duplicated. Cancelling the collector closes the subscription. */
    override fun subscribe(filters: List<Filter>, relays: Collection<String>): Flow<Event> = flow {
        val subscriptionId = newSubscriptionId()
        val seen = HashSet<String>()
        merge(*relays.map { stream(it, subscriptionId, filters, untilEose = false) }.toTypedArray())
            .onCompletion { relays.forEach { connection(it).send(ClientMessage.Close(subscriptionId)) } }
            .collect { if (seen.add(it.id)) emit(it) }
    }

    /** Sends [event] to [relays] and waits up to [timeout] for each relay's `OK`. */
    override suspend fun publish(event: Event, relays: Collection<String>, timeout: Duration): PublishResult {
        val accepted = mutableSetOf<String>()
        val rejected = mutableMapOf<String, String>()
        withTimeoutOrNull(timeout) {
            merge(
                *relays.map { url ->
                    val connection = connection(url)
                    val answer = connection.messages
                        .onSubscription { connection.send(ClientMessage.Publish(event)) }
                        .filterIsInstance<RelayMessage.Ok>()
                        .filter { it.eventId == event.id }
                        .map { url to it }
                    answer.orUnreachable(connection)
                }.toTypedArray(),
            ).collect { (url, ok) -> if (ok.accepted) accepted += url else rejected[url] = ok.message }
        }
        return PublishResult(accepted, rejected, noAnswer = relays.toSet() - accepted - rejected.keys)
    }

    /** NIP-45 count: the highest count any relay reports, or null when no relay supports it. */
    override suspend fun count(filters: List<Filter>, relays: Collection<String>, timeout: Duration): Long? {
        val subscriptionId = newSubscriptionId()
        var best: Long? = null
        withTimeoutOrNull(timeout) {
            merge(
                *relays.map { url ->
                    val connection = connection(url)
                    val answer = connection.messages
                        .onSubscription { connection.send(ClientMessage.Count(subscriptionId, filters)) }
                        .filterIsInstance<RelayMessage.ForSubscription>()
                        .filter { it.subscriptionId == subscriptionId }
                    answer.orUnreachable(connection)
                }.toTypedArray(),
            ).collect { message ->
                if (message is RelayMessage.CountResult) best = maxOf(best ?: 0, message.count)
            }
        }
        return best
    }

    private fun stream(url: String, subscriptionId: String, filters: List<Filter>, untilEose: Boolean): Flow<Event> {
        val connection = connection(url)
        val messages: Flow<RelayMessage.ForSubscription?> = connection.messages
            .onSubscription { connection.send(ClientMessage.Req(subscriptionId, filters)) }
            .filterIsInstance<RelayMessage.ForSubscription>()
            .filter { it.subscriptionId == subscriptionId }
        // A one-shot query gives up on a relay that cannot be reached; a live subscription waits for the reconnect.
        val untilAnswered = if (untilEose) merge(messages, connection.unreachable().map { null }) else messages
        return untilAnswered
            .transformWhile { message ->
                when (message) {
                    null -> false
                    is RelayMessage.EventMessage -> {
                        if (verify(message.event)) emit(message.event)
                        true
                    }
                    is RelayMessage.EndOfStoredEvents -> !untilEose
                    // A live subscription outlives "auth-required": it is sent again once the relay accepts AUTH.
                    is RelayMessage.Closed -> !untilEose && message.message.startsWith("auth-required")
                    is RelayMessage.CountResult -> true
                }
            }
            .flowOn(verifyDispatcher)
    }

    /** The first answer, or nothing once the relay is unreachable. */
    private fun <T : Any> Flow<T>.orUnreachable(connection: RelayConnection): Flow<T> =
        merge(this, connection.unreachable().map { null }).take(1).filterNotNull()

    private fun newSubscriptionId(): String = Random.nextLong().toULong().toString(16)
}
