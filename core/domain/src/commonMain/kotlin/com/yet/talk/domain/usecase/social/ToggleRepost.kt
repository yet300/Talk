package com.yet.talk.domain.usecase.social

import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.repository.ReactionRepository
import dev.zacsweers.metro.Inject

@Inject
class ToggleRepost(private val reactions: ReactionRepository) {
    suspend operator fun invoke(post: Post): Result<Unit> = reactions.setReposted(post.id, !post.viewer.reposted)
}
