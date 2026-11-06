package com.yet.talk.feature.profile.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.FollowRepository
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.domain.usecase.social.ToggleBookmark
import com.yet.talk.domain.usecase.social.ToggleFollow
import com.yet.talk.domain.usecase.social.ToggleLike
import com.yet.talk.domain.usecase.social.ToggleRepost
import com.yet.talk.feature.profile.ProfileComponent
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Inject
internal class ProfileStoreFactory(
    private val storeFactory: StoreFactory,
    private val accounts: AccountRepository,
    private val profiles: ProfileRepository,
    private val follows: FollowRepository,
    private val toggleFollow: ToggleFollow,
    private val toggleLike: ToggleLike,
    private val toggleRepost: ToggleRepost,
    private val toggleBookmark: ToggleBookmark,
) {
    fun create(userId: UserId?): ProfileStore =
        object : ProfileStore, Store<ProfileStore.Intent, ProfileStore.State, Nothing> by storeFactory.create(
            name = "ProfileStore",
            initialState = ProfileStore.State(requested = userId),
            bootstrapper = SimpleBootstrapper(ProfileStore.Action.Start),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl :
        CoroutineExecutor<ProfileStore.Intent, ProfileStore.Action, ProfileStore.State, ProfileStore.Msg, Nothing>() {

        override fun executeAction(action: ProfileStore.Action) {
            scope.launch { accounts.account.collect { dispatch(ProfileStore.Msg.ViewerChanged(it?.userId)) } }
            val ids = state().requested?.let { flowOf(it) } ?: accounts.account.map { it?.userId }
            scope.launch {
                ids.distinctUntilChanged().collectLatest { id ->
                    dispatch(ProfileStore.Msg.UserIdChanged(id))
                    if (id != null) observe(id)
                }
            }
        }

        private suspend fun observe(id: UserId) = coroutineScope {
            launch { profiles.observeUser(id).collect { dispatch(ProfileStore.Msg.UserChanged(it)) } }
            launch { profiles.observeStats(id).collect { dispatch(ProfileStore.Msg.StatsChanged(it)) } }
            launch { follows.observeIsFollowing(id).collect { dispatch(ProfileStore.Msg.FollowingChanged(it)) } }
            launch { profiles.refresh(id).onFailure { dispatch(ProfileStore.Msg.Failed(it as? DomainError)) } }
        }

        override fun executeIntent(intent: ProfileStore.Intent) {
            when (intent) {
                // Bookmarks are the viewer's own list: the tab exists only on their own profile.
                is ProfileStore.Intent.SelectTab ->
                    if (intent.tab != ProfileComponent.Tab.Bookmarks || state().isMe) dispatch(ProfileStore.Msg.TabSelected(intent.tab))
                ProfileStore.Intent.ToggleFollow -> state().userId?.let { id -> run { toggleFollow(id, state().isFollowing) } }
                ProfileStore.Intent.Refresh -> state().userId?.let { id -> run { profiles.refresh(id) } }
                is ProfileStore.Intent.Like -> run { toggleLike(intent.post) }
                is ProfileStore.Intent.Repost -> run { toggleRepost(intent.post) }
                is ProfileStore.Intent.Bookmark -> run { toggleBookmark(intent.post) }
                ProfileStore.Intent.DismissError -> dispatch(ProfileStore.Msg.Failed(null))
            }
        }

        private fun run(action: suspend () -> Result<Unit>) {
            scope.launch { action().onFailure { dispatch(ProfileStore.Msg.Failed(it as? DomainError)) } }
        }
    }

    private object ReducerImpl : Reducer<ProfileStore.State, ProfileStore.Msg> {
        override fun ProfileStore.State.reduce(msg: ProfileStore.Msg): ProfileStore.State =
            when (msg) {
                is ProfileStore.Msg.ViewerChanged -> copy(viewer = msg.viewer)
                is ProfileStore.Msg.UserIdChanged -> copy(userId = msg.id, user = null)
                is ProfileStore.Msg.UserChanged -> copy(user = msg.user)
                is ProfileStore.Msg.StatsChanged -> copy(stats = msg.stats)
                is ProfileStore.Msg.FollowingChanged -> copy(isFollowing = msg.following)
                is ProfileStore.Msg.TabSelected -> copy(tab = msg.tab)
                is ProfileStore.Msg.Failed -> copy(error = msg.error)
            }
    }
}
