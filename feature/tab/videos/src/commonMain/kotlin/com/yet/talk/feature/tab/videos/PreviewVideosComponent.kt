package com.yet.talk.feature.tab.videos

import androidx.paging.PagingData
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.video.Video
import com.yet.talk.domain.model.video.VideoFilters
import com.yet.talk.domain.model.video.VideoList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** For UI previews: a fixed model, actions do nothing. */
class PreviewVideosComponent(
    model: VideosComponent.Model = VideosComponent.Model(
        list = null, catalog = VideosComponent.CatalogState.Ready, forYou = emptyList(), listVideos = emptyList(),
        categories = emptyList(), filters = VideoFilters(),
    ),
) : VideosComponent {
    override val model: Value<VideosComponent.Model> = MutableValue(model)

    override val browse: Flow<PagingData<Video>> = flowOf(PagingData.empty())

    override fun onListSelected(list: VideoList?) = Unit

    override fun onCategorySelected(category: String?) = Unit

    override fun onLevelSelected(level: String?) = Unit

    override fun onTypeSelected(type: String?) = Unit

    override fun onOpenVideo(id: String) = Unit

    override fun onRetry() = Unit
}
