package com.yet.talk.data.di

import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.data.relay.DataRelaySelection
import com.yet.talk.data.repository.AccountRepositoryImpl
import com.yet.talk.data.repository.ActivityRepositoryImpl
import com.yet.talk.data.repository.BookmarkRepositoryImpl
import com.yet.talk.data.repository.FollowRepositoryImpl
import com.yet.talk.data.repository.MediaRepositoryImpl
import com.yet.talk.data.repository.PostRepositoryImpl
import com.yet.talk.data.repository.ProfileRepositoryImpl
import com.yet.talk.data.repository.GitHubRepositoryImpl
import com.yet.talk.data.repository.ReactionRepositoryImpl
import com.yet.talk.data.repository.RelayRepositoryImpl
import com.yet.talk.data.repository.SearchRepositoryImpl
import com.yet.talk.data.repository.ShareLinksImpl
import com.yet.talk.data.repository.TrendingRepositoryImpl
import com.yet.talk.data.session.Session
import com.yet.talk.data.session.VaultSession
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.ActivityRepository
import com.yet.talk.domain.repository.BookmarkRepository
import com.yet.talk.domain.repository.FollowRepository
import com.yet.talk.domain.repository.MediaRepository
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.domain.repository.GitHubRepository
import com.yet.talk.domain.repository.ReactionRepository
import com.yet.talk.domain.repository.RelayRepository
import com.yet.talk.domain.repository.SearchRepository
import com.yet.talk.domain.repository.ShareLinks
import com.yet.talk.domain.repository.TrendingRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphPrivate

/**
 * Data-layer bindings contributed to the app-wide [AppScope] graph.
 *
 * Every implementation is `internal`: features and apps only see the domain repository interfaces.
 * The key vault is bound separately in [KeyVaultBindings] so tests can replace it.
 */
@ContributesTo(AppScope::class)
@BindingContainer
abstract class DataBindings {

    @Binds
    internal abstract val AccountRepositoryImpl.bindAccountRepository: AccountRepository

    @Binds
    internal abstract val PostRepositoryImpl.bindPostRepository: PostRepository

    @Binds
    internal abstract val ProfileRepositoryImpl.bindProfileRepository: ProfileRepository

    @Binds
    internal abstract val GitHubRepositoryImpl.bindGitHubRepository: GitHubRepository

    @Binds
    internal abstract val TrendingRepositoryImpl.bindTrendingRepository: TrendingRepository

    @Binds
    internal abstract val ReactionRepositoryImpl.bindReactionRepository: ReactionRepository

    @Binds
    internal abstract val BookmarkRepositoryImpl.bindBookmarkRepository: BookmarkRepository

    @Binds
    internal abstract val FollowRepositoryImpl.bindFollowRepository: FollowRepository

    @Binds
    internal abstract val ActivityRepositoryImpl.bindActivityRepository: ActivityRepository

    @Binds
    internal abstract val MediaRepositoryImpl.bindMediaRepository: MediaRepository

    @Binds
    internal abstract val RelayRepositoryImpl.bindRelayRepository: RelayRepository

    @Binds
    internal abstract val SearchRepositoryImpl.bindSearchRepository: SearchRepository

    @Binds
    internal abstract val ShareLinksImpl.bindShareLinks: ShareLinks

    /** The signed-in key: only the data layer may reach it. */
    @GraphPrivate
    @Binds
    internal abstract val VaultSession.bindSession: Session

    @Binds
    internal abstract val DataRelaySelection.bindRelaySelection: RelaySelection
}
