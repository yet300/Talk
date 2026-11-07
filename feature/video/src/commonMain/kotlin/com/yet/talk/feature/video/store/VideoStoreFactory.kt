package com.yet.talk.feature.video.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.VideoRepository
import com.yet.talk.feature.video.store.VideoStore.Intent
import com.yet.talk.feature.video.store.VideoStore.Msg
import com.yet.talk.feature.video.store.VideoStore.State
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

@Inject
internal class VideoStoreFactory(private val storeFactory: StoreFactory, private val videos: VideoRepository) {

    fun create(id: String): VideoStore =
        object : VideoStore, Store<Intent, State, Nothing> by storeFactory.create(
            name = "VideoStore",
            initialState = State(id),
            bootstrapper = SimpleBootstrapper(VideoStore.Action.Load),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<Intent, VideoStore.Action, State, Msg, Nothing>() {

        override fun executeAction(action: VideoStore.Action) {
            val id = state().id
            scope.launch {
                // Opened from a post card, the catalog may not be loaded yet; outside it, YouTube's title shows.
                videos.ensureCatalog()
                launch { videos.observeVideo(id).collect { dispatch(Msg.VideoLoaded(it)) } }
                launch { videos.related(id, RELATED).collect { dispatch(Msg.RelatedChanged(it)) } }
            }
            scope.launch { videos.observeViewer(id).collect { dispatch(Msg.ViewerChanged(it)) } }
            scope.launch { videos.observeStats(id).collect { dispatch(Msg.StatsChanged(it)) } }
            scope.launch { videos.comments(id).collect { dispatch(Msg.CommentsChanged(it)) } }
            scope.launch { videos.refresh(id) }
        }

        override fun executeIntent(intent: Intent) {
            val state = state()
            when (intent) {
                Intent.Play -> {
                    dispatch(Msg.Playing)
                    scope.launch { videos.markWatched(state.id) }
                }
                Intent.ToggleLike -> scope.launch {
                    videos.setLiked(state.id, !state.viewer.liked).onFailure { dispatch(Msg.Failed(it as? DomainError)) }
                }
                Intent.ToggleWatchLater -> scope.launch { videos.setWatchLater(state.id, !state.viewer.watchLater) }
                is Intent.ChangeComment -> dispatch(Msg.CommentChanged(intent.text))
                Intent.SendComment -> send(state)
                Intent.DismissError -> dispatch(Msg.Failed(null))
            }
        }

        private fun send(state: State) {
            if (state.isSending || state.comment.isBlank()) return
            dispatch(Msg.Sending(true))
            scope.launch {
                val result = videos.comment(state.id, state.comment)
                dispatch(Msg.Sending(false))
                result.onSuccess { dispatch(Msg.CommentChanged("")) }.onFailure { dispatch(Msg.Failed(it as? DomainError)) }
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.VideoLoaded -> copy(video = msg.video)
            Msg.Playing -> copy(isPlaying = true)
            is Msg.ViewerChanged -> copy(viewer = msg.viewer)
            is Msg.StatsChanged -> copy(stats = msg.stats)
            is Msg.CommentsChanged -> copy(comments = msg.comments)
            is Msg.RelatedChanged -> copy(related = msg.related)
            is Msg.CommentChanged -> copy(comment = msg.text)
            is Msg.Sending -> copy(isSending = msg.sending)
            is Msg.Failed -> copy(error = msg.error)
        }
    }

    private companion object {
        const val RELATED = 3
    }
}
