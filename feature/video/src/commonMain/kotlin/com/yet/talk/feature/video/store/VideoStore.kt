package com.yet.talk.feature.video.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.video.Video
import com.yet.talk.domain.model.video.VideoComment
import com.yet.talk.domain.model.video.VideoStats
import com.yet.talk.domain.model.video.VideoViewer

internal interface VideoStore : Store<VideoStore.Intent, VideoStore.State, Nothing> {

    data class State(
        val id: String,
        val video: Video? = null,
        val isPlaying: Boolean = false,
        val viewer: VideoViewer = VideoViewer(),
        val stats: VideoStats = VideoStats(),
        val comments: List<VideoComment> = emptyList(),
        val related: List<Video> = emptyList(),
        val comment: String = "",
        val isSending: Boolean = false,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data object Play : Intent

        data object ToggleLike : Intent

        data object ToggleWatchLater : Intent

        data class ChangeComment(val text: String) : Intent

        data object SendComment : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object Load : Action
    }

    sealed interface Msg {
        data class VideoLoaded(val video: Video?) : Msg

        data object Playing : Msg

        data class ViewerChanged(val viewer: VideoViewer) : Msg

        data class StatsChanged(val stats: VideoStats) : Msg

        data class CommentsChanged(val comments: List<VideoComment>) : Msg

        data class RelatedChanged(val related: List<Video>) : Msg

        data class CommentChanged(val text: String) : Msg

        data class Sending(val sending: Boolean) : Msg

        data class Failed(val error: DomainError?) : Msg
    }
}
