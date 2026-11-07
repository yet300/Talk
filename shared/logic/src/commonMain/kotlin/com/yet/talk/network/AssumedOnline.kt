package com.yet.talk.network

import com.yet.talk.domain.repository.ConnectivitySource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Android and iOS until their apps watch the system's network: offline shows from the relays' states alone. */
internal class AssumedOnline : ConnectivitySource {
    override val online: Flow<Boolean> = flowOf(true)
}
