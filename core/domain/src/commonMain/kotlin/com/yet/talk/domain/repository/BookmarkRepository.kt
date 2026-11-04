package com.yet.talk.domain.repository

import androidx.paging.PagingData
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    suspend fun setBookmarked(postId: PostId, bookmarked: Boolean): Result<Unit>

    fun bookmarks(): Flow<PagingData<Post>>
}
