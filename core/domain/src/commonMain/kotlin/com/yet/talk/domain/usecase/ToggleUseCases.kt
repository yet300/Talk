package com.yet.talk.domain.usecase

import com.yet.talk.domain.model.Post
import com.yet.talk.domain.model.UserId
import com.yet.talk.domain.repository.BookmarkRepository
import com.yet.talk.domain.repository.FollowRepository
import com.yet.talk.domain.repository.ReactionRepository
import dev.zacsweers.metro.Inject

/** The toggles flip what the viewer currently sees, as KarpathyTalk's buttons do. */
@Inject
class ToggleLike(private val reactions: ReactionRepository) {
    suspend operator fun invoke(post: Post): Result<Unit> = reactions.setLiked(post.id, !post.viewer.liked)
}

@Inject
class ToggleRepost(private val reactions: ReactionRepository) {
    suspend operator fun invoke(post: Post): Result<Unit> = reactions.setReposted(post.id, !post.viewer.reposted)
}

@Inject
class ToggleBookmark(private val bookmarks: BookmarkRepository) {
    suspend operator fun invoke(post: Post): Result<Unit> = bookmarks.setBookmarked(post.id, !post.viewer.bookmarked)
}

@Inject
class ToggleFollow(private val follows: FollowRepository) {
    suspend operator fun invoke(user: UserId, currentlyFollowing: Boolean): Result<Unit> =
        follows.setFollowing(user, !currentlyFollowing)
}
