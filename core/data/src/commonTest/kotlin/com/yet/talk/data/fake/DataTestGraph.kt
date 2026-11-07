package com.yet.talk.data.fake

import com.app.common.AppDispatchers
import com.app.common.di.CommonBindings
import com.yet.talk.api.di.ApiBindings
import com.yet.talk.api.source.media.MediaRemoteSource
import com.yet.talk.api.source.nip05.Nip05Verifier
import com.yet.talk.api.source.github.GitHubRemoteSource
import com.yet.talk.api.source.youtube.YouTubeRemoteSource
import com.yet.talk.data.video.catalog.VideoCatalogSource
import com.yet.talk.data.di.VideoCatalogBindings
import com.yet.talk.api.source.video.VideoRemoteSource
import com.yet.talk.domain.repository.VideoRepository
import com.yet.talk.api.source.post.PostRemoteSource
import com.yet.talk.api.source.publish.PublishRemoteSource
import com.yet.talk.api.source.social.SocialRemoteSource
import com.yet.talk.data.di.KeyVaultBindings
import com.yet.talk.data.session.KeyVault
import com.yet.talk.database.dao.SyncDao
import com.yet.talk.database.driver.SqlDriverFactory
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.BookmarkRepository
import com.yet.talk.domain.repository.FollowRepository
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.domain.repository.GitHubRepository
import com.yet.talk.domain.repository.ReactionRepository
import com.yet.talk.domain.repository.TrendingRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Clock

/**
 * The real database and data bindings, as the app graph merges them, with the network, the key
 * vault and the shared app bindings replaced by test doubles.
 */
@DependencyGraph(AppScope::class, excludes = [ApiBindings::class, CommonBindings::class, KeyVaultBindings::class, VideoCatalogBindings::class])
internal interface DataTestGraph {
    val accounts: AccountRepository
    val posts: PostRepository
    val reactions: ReactionRepository
    val bookmarks: BookmarkRepository
    val follows: FollowRepository
    val trending: TrendingRepository
    val profiles: ProfileRepository
    val github: GitHubRepository
    val videos: VideoRepository
    val syncDao: SyncDao

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides driverFactory: SqlDriverFactory,
            @Provides dispatchers: AppDispatchers,
            @Provides scope: CoroutineScope,
            @Provides clock: Clock,
            @Provides vault: KeyVault,
            @Provides postRemote: PostRemoteSource,
            @Provides socialRemote: SocialRemoteSource,
            @Provides publishRemote: PublishRemoteSource,
            @Provides mediaRemote: MediaRemoteSource,
            @Provides nip05: Nip05Verifier,
            @Provides github: GitHubRemoteSource,
            @Provides youtube: YouTubeRemoteSource,
            @Provides videoRemote: VideoRemoteSource,
            @Provides catalog: VideoCatalogSource,
        ): DataTestGraph
    }
}
