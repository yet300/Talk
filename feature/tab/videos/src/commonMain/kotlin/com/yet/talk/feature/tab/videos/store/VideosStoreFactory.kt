package com.yet.talk.feature.tab.videos.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.video.VideoList
import com.yet.talk.domain.repository.VideoRepository
import com.yet.talk.feature.tab.videos.VideosComponent.CatalogState
import com.yet.talk.feature.tab.videos.store.VideosStore.Intent
import com.yet.talk.feature.tab.videos.store.VideosStore.Msg
import com.yet.talk.feature.tab.videos.store.VideosStore.State
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Inject
internal class VideosStoreFactory(private val storeFactory: StoreFactory, private val videos: VideoRepository) {

    fun create(): VideosStore =
        object : VideosStore, Store<Intent, State, Nothing> by storeFactory.create(
            name = "VideosStore",
            initialState = State(),
            bootstrapper = SimpleBootstrapper(VideosStore.Action.Load),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<Intent, VideosStore.Action, State, Msg, Nothing>() {
        private var listJob: Job? = null

        override fun executeAction(action: VideosStore.Action) {
            scope.launch { videos.forYou(FOR_YOU).collect { dispatch(Msg.ForYouChanged(it)) } }
            scope.launch { videos.categories().collect { dispatch(Msg.CategoriesChanged(it)) } }
            load()
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.SelectList -> selectList(intent.list)
                is Intent.SetFilters -> dispatch(Msg.FiltersChanged(intent.filters))
                Intent.Retry -> load()
            }
        }

        /** The first time, the catalog loads into the database (a few seconds on the web); afterwards it is there. */
        private fun load() {
            dispatch(Msg.CatalogChanged(CatalogState.Loading))
            scope.launch {
                videos.ensureCatalog()
                    .onSuccess { dispatch(Msg.CatalogChanged(CatalogState.Ready)) }
                    .onFailure { dispatch(Msg.CatalogChanged(CatalogState.Failed(it as? DomainError ?: DomainError.Network(it)))) }
            }
        }

        private fun selectList(list: VideoList?) {
            dispatch(Msg.ListSelected(list))
            listJob?.cancel()
            listJob = scope.launch { (list?.let(videos::list) ?: emptyFlow()).collect { dispatch(Msg.ListVideosChanged(it)) } }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.ListSelected -> copy(list = msg.list, listVideos = emptyList())
            is Msg.CatalogChanged -> copy(catalog = msg.catalog)
            is Msg.ForYouChanged -> copy(forYou = msg.videos)
            is Msg.ListVideosChanged -> copy(listVideos = msg.videos)
            is Msg.CategoriesChanged -> copy(categories = msg.categories)
            is Msg.FiltersChanged -> copy(filters = msg.filters)
        }
    }

    private companion object {
        const val FOR_YOU = 3
    }
}

