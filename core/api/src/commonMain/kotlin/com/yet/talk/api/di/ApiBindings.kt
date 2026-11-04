package com.yet.talk.api.di

import com.yet.talk.nostr.relay.KtorRelayTransport
import com.yet.talk.nostr.relay.RelayClient
import com.yet.talk.nostr.relay.RelayPool
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope

/** One relay pool for the app. The platform graph provides the [HttpClient] (with WebSockets) and the app scope. */
@ContributesTo(AppScope::class)
@BindingContainer
object ApiBindings {
    @SingleIn(AppScope::class)
    @Provides
    fun relayClient(http: HttpClient, scope: CoroutineScope): RelayClient = RelayPool(KtorRelayTransport(http), scope)
}
