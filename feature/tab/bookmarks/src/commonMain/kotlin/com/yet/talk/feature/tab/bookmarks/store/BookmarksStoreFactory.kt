package com.yet.talk.feature.tab.bookmarks.store

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
internal class BookmarksStoreFactory(
    private val storeFactory: StoreFactory,
    private val toggleLike: ToggleLike,
    private val toggleRepost: ToggleRepost,
    private val toggleBookmark: ToggleBookmark,
) {
    fun create(): BookmarksStore =
        object : BookmarksStore, Store<BookmarksStore.Intent, BookmarksStore.State, Nothing> by storeFactory.create(
            name = "BookmarksStore",
            initialState = BookmarksStore.State(),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl :
        CoroutineExecutor<BookmarksStore.Intent, Nothing, BookmarksStore.State, BookmarksStore.Msg, Nothing>() {

        override fun executeIntent(intent: BookmarksStore.Intent) {
            when (intent) {
                is BookmarksStore.Intent.Like -> run { toggleLike(intent.post) }
                is BookmarksStore.Intent.Repost -> run { toggleRepost(intent.post) }
                is BookmarksStore.Intent.Bookmark -> run { toggleBookmark(intent.post) }
                BookmarksStore.Intent.DismissError -> dispatch(BookmarksStore.Msg.Failed(null))
            }
        }

        /** The list updates from the database; only failures need handling. */
        private fun run(action: suspend () -> Result<Unit>) {
            scope.launch { action().onFailure { dispatch(BookmarksStore.Msg.Failed(it as? DomainError)) } }
        }
    }

    private object ReducerImpl : Reducer<BookmarksStore.State, BookmarksStore.Msg> {
        override fun BookmarksStore.State.reduce(msg: BookmarksStore.Msg): BookmarksStore.State =
            when (msg) {
                is BookmarksStore.Msg.Failed -> copy(error = msg.error)
            }
    }
}
