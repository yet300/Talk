package com.yet.talk.domain.repository

import com.yet.talk.domain.model.Pager
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId

interface BookmarkRepository {
    suspend fun setBookmarked(postId: PostId, bookmarked: Boolean): Result<Unit>

    fun bookmarks(): Pager<Post>
}
