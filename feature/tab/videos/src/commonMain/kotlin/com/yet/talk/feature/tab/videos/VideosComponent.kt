package com.yet.talk.feature.tab.videos

import androidx.paging.PagingData
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.video.Video
import com.yet.talk.domain.model.video.VideoFilters
import com.yet.talk.domain.model.video.VideoList
import kotlinx.coroutines.flow.Flow

/**
 * Videos (`VideoHome`): the HackerTube catalog. Chips Videos · Liked · Watch later · History; on Videos, For you
 * and Browse with category chips and the Level and Type menus.
 */
interface VideosComponent {
    val model: Value<Model>

    /** Browse with the current filters, 20 a page; cached for the component's lifetime. */
    val browse: Flow<PagingData<Video>>

    /** [list] null: the Videos chip (For you and Browse). */
    fun onListSelected(list: VideoList?)

    fun onCategorySelected(category: String?)

    fun onLevelSelected(level: String?)

    fun onTypeSelected(type: String?)

    fun onOpenVideo(id: String)

    /** Loads the catalog again after it failed. */
    fun onRetry()

    data class Model(
        val list: VideoList?,
        val catalog: CatalogState,
        val forYou: List<Video>,
        /** The selected list's videos (Liked, Watch later, History). */
        val listVideos: List<Video>,
        val categories: List<String>,
        val filters: VideoFilters,
    )

    sealed interface CatalogState {
        data object Loading : CatalogState

        data object Ready : CatalogState

        data class Failed(val error: DomainError) : CatalogState
    }

    sealed interface Output {
        data class OpenVideo(val id: String) : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, output: (Output) -> Unit): VideosComponent
    }
}
