package com.yet.talk.feature.video

import com.app.common.decompose.asValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.feature.video.mapper.stateToModel
import com.yet.talk.feature.video.store.VideoStore
import com.yet.talk.feature.video.store.VideoStoreFactory
import dev.zacsweers.metro.Inject

internal class DefaultVideoComponent(
    componentContext: ComponentContext,
    videoId: String,
    private val output: (VideoComponent.Output) -> Unit,
    storeFactory: VideoStoreFactory,
) : VideoComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create(videoId) }

    override val model: Value<VideoComponent.Model> = store.asValue().map(stateToModel)

    override fun onPlay() = store.accept(VideoStore.Intent.Play)

    override fun onLike() = store.accept(VideoStore.Intent.ToggleLike)

    override fun onWatchLater() = store.accept(VideoStore.Intent.ToggleWatchLater)

    override fun onShare() = output(VideoComponent.Output.Share(model.value.url))

    override fun onCommentChanged(text: String) = store.accept(VideoStore.Intent.ChangeComment(text))

    override fun onSendComment() = store.accept(VideoStore.Intent.SendComment)

    override fun onOpenVideo(id: String) = output(VideoComponent.Output.OpenVideo(id))

    override fun onOpenAuthor(id: UserId) = output(VideoComponent.Output.OpenProfile(id))

    override fun onBack() = output(VideoComponent.Output.Back)

    override fun onErrorShown() = store.accept(VideoStore.Intent.DismissError)
}

@Inject
internal class DefaultVideoComponentFactory(private val storeFactory: VideoStoreFactory) : VideoComponent.Factory {
    override fun invoke(componentContext: ComponentContext, videoId: String, output: (VideoComponent.Output) -> Unit): VideoComponent =
        DefaultVideoComponent(componentContext, videoId, output, storeFactory)
}
