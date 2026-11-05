package com.yet.talk.feature.post.component.revisions.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.repository.PostRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

@Inject
internal class RevisionsStoreFactory(
    private val storeFactory: StoreFactory,
    private val posts: PostRepository,
) {
    fun create(id: PostId): RevisionsStore =
        object : RevisionsStore, Store<RevisionsStore.Intent, RevisionsStore.State, Nothing> by storeFactory.create(
            name = "RevisionsStore",
            initialState = RevisionsStore.State(id = id),
            bootstrapper = SimpleBootstrapper(RevisionsStore.Action.Start),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl :
        CoroutineExecutor<RevisionsStore.Intent, RevisionsStore.Action, RevisionsStore.State, RevisionsStore.Msg, Nothing>() {

        override fun executeAction(action: RevisionsStore.Action) {
            scope.launch { posts.observeRevisions(state().id).collect { dispatch(RevisionsStore.Msg.RevisionsChanged(it)) } }
            refresh()
        }

        override fun executeIntent(intent: RevisionsStore.Intent) {
            when (intent) {
                is RevisionsStore.Intent.Select -> dispatch(RevisionsStore.Msg.Selected(intent.number))
                RevisionsStore.Intent.Refresh -> refresh()
                RevisionsStore.Intent.DismissError -> dispatch(RevisionsStore.Msg.Failed(null))
            }
        }

        private fun refresh() {
            dispatch(RevisionsStore.Msg.Loading(true))
            scope.launch {
                val error = posts.refreshRevisions(state().id).exceptionOrNull() as? DomainError
                dispatch(RevisionsStore.Msg.Loading(false))
                if (error != null) dispatch(RevisionsStore.Msg.Failed(error))
            }
        }
    }

    private object ReducerImpl : Reducer<RevisionsStore.State, RevisionsStore.Msg> {
        override fun RevisionsStore.State.reduce(msg: RevisionsStore.Msg): RevisionsStore.State =
            when (msg) {
                is RevisionsStore.Msg.RevisionsChanged -> copy(revisions = msg.revisions)
                is RevisionsStore.Msg.Selected -> copy(selectedNumber = msg.number)
                is RevisionsStore.Msg.Loading -> copy(isLoading = msg.loading)
                is RevisionsStore.Msg.Failed -> copy(error = msg.error)
            }
    }
}
