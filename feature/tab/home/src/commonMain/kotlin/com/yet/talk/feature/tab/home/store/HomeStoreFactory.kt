package com.yet.talk.feature.tab.home.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.usecase.social.ToggleBookmark
import com.yet.talk.domain.usecase.social.ToggleLike
import com.yet.talk.domain.usecase.social.ToggleRepost
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

@Inject
internal class HomeStoreFactory(
    private val storeFactory: StoreFactory,
    private val toggleLike: ToggleLike,
    private val toggleRepost: ToggleRepost,
    private val toggleBookmark: ToggleBookmark,
) {
    fun create(): HomeStore =
        object : HomeStore, Store<HomeStore.Intent, HomeStore.State, Nothing> by storeFactory.create(
            name = "HomeStore",
            initialState = HomeStore.State(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<HomeStore.Intent, Nothing, HomeStore.State, HomeStore.Msg, Nothing>() {

        override fun executeIntent(intent: HomeStore.Intent) {
            when (intent) {
                is HomeStore.Intent.Like -> run { toggleLike(intent.post) }
                is HomeStore.Intent.Repost -> run { toggleRepost(intent.post) }
                is HomeStore.Intent.Bookmark -> run { toggleBookmark(intent.post) }
                HomeStore.Intent.DismissError -> dispatch(HomeStore.Msg.Failed(null))
            }
        }

        /** The feed updates from the database; only failures need handling. */
        private fun run(action: suspend () -> Result<Unit>) {
            scope.launch { action().onFailure { dispatch(HomeStore.Msg.Failed(it as? DomainError)) } }
        }
    }

    private object ReducerImpl : Reducer<HomeStore.State, HomeStore.Msg> {
        override fun HomeStore.State.reduce(msg: HomeStore.Msg): HomeStore.State =
            when (msg) {
                is HomeStore.Msg.Failed -> copy(error = msg.error)
            }
    }
}
