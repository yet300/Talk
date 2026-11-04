package com.yet.talk.domain.usecase.post

import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.repository.PostRepository
import dev.zacsweers.metro.Inject

@Inject
class ReplyToPost(private val posts: PostRepository) {
    suspend operator fun invoke(parent: PostId, content: String, images: List<Image> = emptyList()): Result<PostId> =
        validated(content, images, isReply = true) { clean -> posts.reply(parent, clean, images) }
}
