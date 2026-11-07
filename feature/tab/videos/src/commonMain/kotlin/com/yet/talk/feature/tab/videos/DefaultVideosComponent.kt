package com.yet.talk.feature.tab.videos

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.app.common.decompose.asValue
import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.yet.talk.domain.model.video.Video
import com.yet.talk.domain.model.video.VideoList
import com.yet.talk.domain.repository.VideoRepository
import com.yet.talk.feature.tab.videos.mapper.stateToModel
import com.yet.talk.feature.tab.videos.store.VideosStore
import com.yet.talk.feature.tab.videos.store.VideosStoreFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultVideosComponent(
    componentContext: ComponentContext,
    private val output: (VideosComponent.Output) -> Unit,
    storeFactory: VideosStoreFactory,
    videoRepository: VideoRepository,
) : VideosComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create() }

    override val model: Value<VideosComponent.Model> = store.asValue().map(stateToModel)

    // Paged only once the catalog is in the database, so the first page is not empty.
    override val browse: Flow<PagingData<Video>> = store.states
        .filter { it.catalog == VideosComponent.CatalogState.Ready }
        .map { it.filters }
        .distinctUntilChanged()
        .flatMapLatest(videoRepository::browse)
        .cachedIn(coroutineScope())

    override fun onListSelected(list: VideoList?) = store.accept(VideosStore.Intent.SelectList(list))

    override fun onCategorySelected(category: String?) =
        store.accept(VideosStore.Intent.SetFilters(store.state.filters.copy(category = category)))

    override fun onLevelSelected(level: String?) = store.accept(VideosStore.Intent.SetFilters(store.state.filters.copy(level = level)))

    override fun onTypeSelected(type: String?) = store.accept(VideosStore.Intent.SetFilters(store.state.filters.copy(type = type)))

    override fun onOpenVideo(id: String) = output(VideosComponent.Output.OpenVideo(id))

    override fun onRetry() = store.accept(VideosStore.Intent.Retry)
}

@Inject
internal class DefaultVideosComponentFactory(
    private val storeFactory: VideosStoreFactory,
    private val videoRepository: VideoRepository,
) : VideosComponent.Factory {
    override fun invoke(componentContext: ComponentContext, output: (VideosComponent.Output) -> Unit): VideosComponent =
        DefaultVideosComponent(componentContext, output, storeFactory, videoRepository)
}
