package com.yet.talk.domain.usecase.social

import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.repository.BookmarkRepository
import dev.zacsweers.metro.Inject

@Inject
class ToggleBookmark(private val bookmarks: BookmarkRepository) {
    suspend operator fun invoke(post: Post): Result<Unit> = bookmarks.setBookmarked(post.id, !post.viewer.bookmarked)
}
