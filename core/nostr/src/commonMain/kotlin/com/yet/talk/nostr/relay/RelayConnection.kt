package com.yet.talk.nostr.relay

import com.yet.talk.nostr.message.ClientMessage
import com.yet.talk.nostr.message.RelayMessage
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * One relay. Connects lazily on the first [send], reconnects with exponential backoff, and on every
 * connect sends the subscriptions that are still open.
 *
 * Subscriptions live in [openSubscriptions], not in the queue, so a relay that is offline for a while
 * is not flooded with stale REQ/CLOSE pairs when it comes back. Other messages (EVENT, COUNT) are
 * queued only while a connection may still succeed; while the relay is [State.Disconnected] they are
 * dropped, and callers see the state change and stop waiting for an answer.
 */
internal class RelayConnection(
    val url: String,
    private val transport: RelayTransport,
    scope: CoroutineScope,
    private val backoff: (attempt: Int) -> Duration = ::defaultBackoff,
    private val authenticator: RelayAuthenticator? = null,
) {
    enum class State { Idle, Connecting, Connected, Disconnected }

    private val _messages = MutableSharedFlow<RelayMessage>(extraBufferCapacity = 1024)

    /** Everything the relay sends. Hot: subscribe before sending the request that triggers a reply. */
    val messages: SharedFlow<RelayMessage> = _messages.asSharedFlow()

    private val _state = MutableStateFlow(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val outgoing = Channel<ClientMessage>(Channel.UNLIMITED)
    private val openSubscriptions = MutableStateFlow<Map<String, ClientMessage.Req>>(emptyMap())
    private val loop = scope.launch(start = CoroutineStart.LAZY) { run() }

    fun send(message: ClientMessage) {
        loop.start()
        when (message) {
            is ClientMessage.Req -> {
                openSubscriptions.update { it + (message.subscriptionId to message) }
                if (_state.value == State.Connected) outgoing.trySend(message)
            }
            is ClientMessage.Close -> {
                val wasOpen = message.subscriptionId in openSubscriptions.value
                openSubscriptions.update { it - message.subscriptionId }
                if (wasOpen && _state.value == State.Connected) outgoing.trySend(message)
            }
            else -> if (_state.value != State.Disconnected) outgoing.trySend(message)
        }
    }

    private suspend fun run() {
        var attempt = 0
        while (currentCoroutineContext().isActive) {
            _state.value = State.Connecting
            try {
                val socket = transport.connect(url)
                attempt = 0
                try {
                    coroutineScope {
                        val sender = launch {
                            openSubscriptions.value.values.forEach { socket.send(it.toJson()) }
                            for (message in outgoing) socket.send(message.toJson())
                        }
                        _state.value = State.Connected
                        var authId: String? = null
                        socket.incoming.collect { text ->
                            val message = RelayMessage.parse(text) ?: return@collect
                            when {
                                // NIP-42: answer, then ask again for what the relay refused before it knew us.
                                message is RelayMessage.Auth && authenticator != null -> launch {
                                    val answer = authenticator.authenticate(url, message.challenge) ?: return@launch
                                    authId = answer.id
                                    outgoing.trySend(ClientMessage.Auth(answer))
                                }
                                message is RelayMessage.Ok && message.eventId == authId && message.accepted ->
                                    openSubscriptions.value.values.forEach { outgoing.trySend(it) }
                            }
                            _messages.emit(message)
                        }
                        sender.cancel()
                    }
                } finally {
                    withContext(NonCancellable) { runCatching { socket.close() } }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Unreachable relay or dropped socket: retry below.
            }
            _state.value = State.Disconnected
            // Anything still queued was meant for the lost connection; open subscriptions are re-sent on connect.
            while (outgoing.tryReceive().isSuccess) Unit
            // Back off, unless the network comes back first (reconnectNow).
            withTimeoutOrNull(backoff(attempt++)) { wakeUps.receive() }
        }
    }

    private val wakeUps = Channel<Unit>(Channel.CONFLATED)

    /** The device is online again: a relay waiting out its backoff tries at once. */
    fun reconnectNow() {
        wakeUps.trySend(Unit)
    }

    /** Emits once the relay is unreachable (now or later): a caller waiting for an answer can stop. */
    fun unreachable(): Flow<Unit> = state.filter { it == State.Disconnected }.map { }
}

internal fun defaultBackoff(attempt: Int): Duration = (1 shl attempt.coerceAtMost(6)).seconds
