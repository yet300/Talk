package com.yet.talk.network

import com.yet.talk.domain.repository.ConnectivitySource
import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * The system's network (jordond/connectivity: `ConnectivityManager` on Android, `NWPathMonitor` on iOS), watched for
 * the app's lifetime.
 */
internal class DeviceConnectivity : ConnectivitySource {
    // Created on first use: resolving the app graph must not touch the platform (Android needs its context here).
    private val connectivity by lazy { Connectivity { autoStart = true } }

    override val online: Flow<Boolean> = flow {
        emit(connectivity.status())
        emitAll(connectivity.statusUpdates)
    }.map { it.isConnected }.distinctUntilChanged()
}
