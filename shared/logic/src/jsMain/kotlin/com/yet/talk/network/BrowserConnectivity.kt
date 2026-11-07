package com.yet.talk.network

import com.yet.talk.domain.repository.ConnectivitySource
import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.w3c.dom.events.Event

/** `navigator.onLine`, updated by the `online` and `offline` events. */
internal class BrowserConnectivity : ConnectivitySource {
    override val online: Flow<Boolean> = callbackFlow {
        val listener: (Event) -> Unit = { trySend(window.navigator.onLine) }
        trySend(window.navigator.onLine)
        window.addEventListener("online", listener)
        window.addEventListener("offline", listener)
        awaitClose {
            window.removeEventListener("online", listener)
            window.removeEventListener("offline", listener)
        }
    }.distinctUntilChanged()
}
