package com.yet.talk.di

import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.ActivityRepository
import com.yet.talk.domain.repository.BookmarkRepository
import com.yet.talk.domain.repository.FollowRepository
import com.yet.talk.domain.repository.MediaRepository
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.domain.repository.ReactionRepository
import com.yet.talk.domain.repository.RelayRepository
import com.yet.talk.domain.repository.TrendingRepository
import com.yet.talk.feature.root.RootComponent

/**
 * What the platform apps take from the graph. The `@DependencyGraph` itself lives in each
 * platform source set so it can see platform-only bindings (database driver, KSafe, HTTP engine).
 */
interface AppGraph {
    val rootComponentFactory: RootComponent.Factory
}

/** Every repository, so tests can check that the whole data layer resolves. Not exported to the apps. */
internal interface RepositoriesGraph {
    val accountRepository: AccountRepository
    val postRepository: PostRepository
    val profileRepository: ProfileRepository
    val trendingRepository: TrendingRepository
    val reactionRepository: ReactionRepository
    val bookmarkRepository: BookmarkRepository
    val followRepository: FollowRepository
    val activityRepository: ActivityRepository
    val mediaRepository: MediaRepository
    val relayRepository: RelayRepository
}
