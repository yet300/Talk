package com.yet.talk.nostr.relay

import com.yet.talk.nostr.ClientMessage
import com.yet.talk.nostr.RelayMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * One relay. Connects lazily on the first [send], reconnects with exponential backoff, and on every
 * reconnect re-sends the subscriptions that are still open. Messages queued while offline are sent
 * once connected.
 */
class RelayConnection internal constructor(
    val url: String,
    private val transport: RelayTransport,
    scope: CoroutineScope,
    private val backoff: (attempt: Int) -> Duration = ::defaultBackoff,
) {
    private val _messages = MutableSharedFlow<RelayMessage>(extraBufferCapacity = 1024)

    /** Everything the relay sends. Hot: subscribe before sending the request that triggers a reply. */
    val messages: SharedFlow<RelayMessage> = _messages.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val outgoing = Channel<ClientMessage>(Channel.UNLIMITED)
    private val openSubscriptions = MutableStateFlow<Map<String, ClientMessage.Req>>(emptyMap())
    private val loop = scope.launch(start = CoroutineStart.LAZY) { run() }

    fun send(message: ClientMessage) {
        when (message) {
            is ClientMessage.Req -> openSubscriptions.update { it + (message.subscriptionId to message) }
            is ClientMessage.Close -> openSubscriptions.update { it - message.subscriptionId }
            else -> Unit
        }
        outgoing.trySend(message)
        loop.start()
    }

    private suspend fun run() {
        var attempt = 0
        var reconnecting = false
        while (currentCoroutineContext().isActive) {
            try {
                val socket = transport.connect(url)
                attempt = 0
                _connected.value = true
                try {
                    coroutineScope {
                        val sender = launch {
                            // A subscription opened while offline is also still queued in `outgoing`; the relay
                            // replaces a subscription with the same id, so sending it twice is harmless.
                            if (reconnecting) openSubscriptions.value.values.forEach { socket.send(it.toJson()) }
                            for (message in outgoing) socket.send(message.toJson())
                        }
                        socket.incoming.collect { text -> RelayMessage.parse(text)?.let { _messages.emit(it) } }
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
            _connected.value = false
            reconnecting = true
            delay(backoff(attempt++))
        }
    }
}

internal fun defaultBackoff(attempt: Int): Duration = (1 shl attempt.coerceAtMost(6)).seconds
