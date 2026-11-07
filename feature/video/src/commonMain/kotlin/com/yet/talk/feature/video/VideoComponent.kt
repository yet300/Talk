package com.yet.talk.feature.video

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.model.video.Video
import com.yet.talk.domain.model.video.VideoComment
import com.yet.talk.domain.model.video.VideoStats
import com.yet.talk.domain.model.video.VideoViewer

/** One video (`VideoPage`): the player, details (`VideoInfo`), comments and related videos. */
interface VideoComponent {
    val model: Value<Model>

    /** Starts the player and adds the video to History. */
    fun onPlay()

    fun onLike()

    fun onWatchLater()

    /** Opens the editor with the video's link. */
    fun onShare()

    fun onCommentChanged(text: String)

    fun onSendComment()

    fun onOpenVideo(id: String)

    fun onOpenAuthor(id: UserId)

    fun onBack()

    fun onErrorShown()

    data class Model(
        val id: String,
        /** Null until the catalog or YouTube answers. */
        val video: Video?,
        val isPlaying: Boolean,
        val viewer: VideoViewer,
        val stats: VideoStats,
        val comments: List<VideoComment>,
        val related: List<Video>,
        val comment: String,
        val isSending: Boolean,
        val canSend: Boolean,
        val error: DomainError?,
    ) {
        val url: String get() = "https://www.youtube.com/watch?v=$id"
    }

    sealed interface Output {
        data object Back : Output

        data class OpenVideo(val id: String) : Output

        data class OpenProfile(val id: UserId) : Output

        /** "Share": a new post starting with [text]. */
        data class Share(val text: String) : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, videoId: String, output: (Output) -> Unit): VideoComponent
    }
}
