package com.yet.talk.nostr.di

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

/**
 * The relay pool, contributed to the app-wide [AppScope] graph; only [RelayClient] leaves this module.
 * The platform graph provides the [HttpClient] (with WebSockets) and the app [CoroutineScope].
 */
@ContributesTo(AppScope::class)
@BindingContainer
abstract class NostrBindings {
    companion object {
        /** One pool for the app: relay connections are shared by every caller. */
        @SingleIn(AppScope::class)
        @Provides
        internal fun provideRelayClient(http: HttpClient, scope: CoroutineScope): RelayClient =
            RelayPool(KtorRelayTransport(http), scope)
    }
}
