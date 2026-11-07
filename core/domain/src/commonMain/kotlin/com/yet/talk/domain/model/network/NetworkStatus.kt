package com.yet.talk.domain.model.network

/** A relay of yours, as Settings shows it. */
enum class RelayState { NotUsedYet, Connecting, Connected, Unreachable }

/**
 * Whether Talk can reach the network: the device's connection and your relays' states, and how many of your
 * events are still waiting for a relay that refused or did not answer.
 */
data class NetworkStatus(
    val deviceOnline: Boolean = true,
    /** Your relays (read and write) and their states. */
    val relays: Map<String, RelayState> = emptyMap(),
    val waitingToSend: Int = 0,
) {
    /** No connection, or every relay tried so far is unreachable: showing what is saved. */
    val isOffline: Boolean
        get() = !deviceOnline || (relays.values.none { it == RelayState.Connected || it == RelayState.Connecting } &&
            relays.values.any { it == RelayState.Unreachable })
}
