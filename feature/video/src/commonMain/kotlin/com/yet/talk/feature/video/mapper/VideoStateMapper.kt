package com.yet.talk.feature.video.mapper

import com.yet.talk.feature.video.VideoComponent
import com.yet.talk.feature.video.store.VideoStore

internal val stateToModel: (VideoStore.State) -> VideoComponent.Model = { state ->
    VideoComponent.Model(
        id = state.id,
        video = state.video,
        isPlaying = state.isPlaying,
        viewer = state.viewer,
        stats = state.stats,
        comments = state.comments,
        related = state.related,
        comment = state.comment,
        isSending = state.isSending,
        canSend = state.comment.isNotBlank() && !state.isSending,
        error = state.error,
    )
}
