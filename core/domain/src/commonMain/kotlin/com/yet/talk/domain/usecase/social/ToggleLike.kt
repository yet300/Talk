package com.yet.talk.domain.usecase.social

import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.repository.ReactionRepository
import dev.zacsweers.metro.Inject

/** The toggles flip what the viewer currently sees, as KarpathyTalk's buttons do. */
@Inject
class ToggleLike(private val reactions: ReactionRepository) {
    suspend operator fun invoke(post: Post): Result<Unit> = reactions.setLiked(post.id, !post.viewer.liked)
}
