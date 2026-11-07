package com.yet.talk.domain.repository

import com.yet.talk.domain.model.network.NetworkStatus
import kotlinx.coroutines.flow.Flow

/** The offline indicator and the relays' states. */
interface NetworkRepository {
    val status: Flow<NetworkStatus>
}

/** The device's own idea of being online (the browser's `online` / `offline` events on the web). */
interface ConnectivitySource {
    val online: Flow<Boolean>
}
