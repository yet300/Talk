package com.yet.talk.api.di

import com.yet.talk.api.source.github.GitHubClient
import com.yet.talk.api.source.github.GitHubRemoteSource
import com.yet.talk.api.source.media.BlossomClient
import com.yet.talk.api.source.youtube.YouTubeOEmbedClient
import com.yet.talk.api.source.video.RelayVideoRemoteSource
import com.yet.talk.api.source.channel.ChannelRemoteSource
import com.yet.talk.api.source.relay.RelayClientStatusSource
import com.yet.talk.api.source.relay.RelayStatusSource
import com.yet.talk.api.source.channel.RelayChannelRemoteSource
import com.yet.talk.api.source.chat.ChatRemoteSource
import com.yet.talk.api.source.chat.RelayChatRemoteSource
import com.yet.talk.api.source.video.VideoRemoteSource
import com.yet.talk.api.source.youtube.YouTubeRemoteSource
import com.yet.talk.api.source.media.MediaRemoteSource
import com.yet.talk.api.source.nip05.Nip05Client
import com.yet.talk.api.source.nip05.Nip05Verifier
import com.yet.talk.api.source.post.PostRemoteSource
import com.yet.talk.api.source.post.RelayPostRemoteSource
import com.yet.talk.api.source.publish.PublishRemoteSource
import com.yet.talk.api.source.publish.RelayPublishRemoteSource
import com.yet.talk.api.source.search.RelaySearchRemoteSource
import com.yet.talk.api.source.search.SearchRemoteSource
import com.yet.talk.api.source.social.RelaySocialRemoteSource
import com.yet.talk.api.source.social.SocialRemoteSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

/**
 * Remote-source bindings contributed to the app-wide [AppScope] graph. The implementations are
 * `internal`; only the source interfaces leave this module. They read and write through the
 * [com.yet.talk.nostr.relay.RelayClient] bound by `NostrBindings`.
 */
@ContributesTo(AppScope::class)
@BindingContainer
abstract class ApiBindings {

    @Binds
    internal abstract val RelayPostRemoteSource.bindPostRemoteSource: PostRemoteSource

    @Binds
    internal abstract val RelaySocialRemoteSource.bindSocialRemoteSource: SocialRemoteSource

    @Binds
    internal abstract val RelayPublishRemoteSource.bindPublishRemoteSource: PublishRemoteSource

    @Binds
    internal abstract val BlossomClient.bindMediaRemoteSource: MediaRemoteSource

    @Binds
    internal abstract val Nip05Client.bindNip05Verifier: Nip05Verifier

    @Binds
    internal abstract val GitHubClient.bindGitHubRemoteSource: GitHubRemoteSource

    @Binds
    internal abstract val YouTubeOEmbedClient.bindYouTubeRemoteSource: YouTubeRemoteSource

    @Binds
    internal abstract val RelayVideoRemoteSource.bindVideoRemoteSource: VideoRemoteSource

    @Binds
    internal abstract val RelayChatRemoteSource.bindChatRemoteSource: ChatRemoteSource

    @Binds
    internal abstract val RelayChannelRemoteSource.bindChannelRemoteSource: ChannelRemoteSource

    @Binds
    internal abstract val RelayClientStatusSource.bindRelayStatusSource: RelayStatusSource

    @Binds
    internal abstract val RelaySearchRemoteSource.bindSearchRemoteSource: SearchRemoteSource
}
