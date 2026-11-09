package com.yet.talk.mcp.di

import com.yet.talk.api.source.chat.ChatRemoteSource
import com.yet.talk.api.source.post.PostRemoteSource
import com.yet.talk.api.source.publish.PublishRemoteSource
import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.api.source.search.SearchRemoteSource
import com.yet.talk.api.source.social.SocialRemoteSource
import com.yet.talk.nostr.relay.RelayAuthenticator
import com.yet.talk.nostr.relay.RelayClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope

/** The parts of `core:api` the server talks to relays through. */
internal interface TalkSources {
    val relays: RelayClient
    val posts: PostRemoteSource
    val search: SearchRemoteSource
    val social: SocialRemoteSource
    val publisher: PublishRemoteSource
    val chats: ChatRemoteSource
}

/** The same relay pool and remote sources as the apps; the server adds only its key and its relays. */
@DependencyGraph(AppScope::class)
internal interface McpGraph : TalkSources {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides http: HttpClient,
            @Provides scope: CoroutineScope,
            @Provides authenticator: RelayAuthenticator,
            @Provides selection: RelaySelection,
        ): McpGraph
    }
}
