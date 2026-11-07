package com.yet.talk.feature.tab.videos.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.video.Video
import com.yet.talk.domain.model.video.VideoFilters
import com.yet.talk.domain.model.video.VideoList
import com.yet.talk.feature.tab.videos.VideosComponent.CatalogState

internal interface VideosStore : Store<VideosStore.Intent, VideosStore.State, Nothing> {

    data class State(
        val list: VideoList? = null,
        val catalog: CatalogState = CatalogState.Loading,
        val forYou: List<Video> = emptyList(),
        val listVideos: List<Video> = emptyList(),
        val categories: List<String> = emptyList(),
        val filters: VideoFilters = VideoFilters(),
    )

    sealed interface Intent {
        data class SelectList(val list: VideoList?) : Intent

        data class SetFilters(val filters: VideoFilters) : Intent

        data object Retry : Intent
    }

    sealed interface Action {
        data object Load : Action
    }

    sealed interface Msg {
        data class ListSelected(val list: VideoList?) : Msg

        data class CatalogChanged(val catalog: CatalogState) : Msg

        data class ForYouChanged(val videos: List<Video>) : Msg

        data class ListVideosChanged(val videos: List<Video>) : Msg

        data class CategoriesChanged(val categories: List<String>) : Msg

        data class FiltersChanged(val filters: VideoFilters) : Msg
    }
}
