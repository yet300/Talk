package com.yet.talk.domain.repository

import com.yet.talk.domain.model.post.PostId

interface ReactionRepository {
    suspend fun setLiked(postId: PostId, liked: Boolean): Result<Unit>

    suspend fun setReposted(postId: PostId, reposted: Boolean): Result<Unit>
}
