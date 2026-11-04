package com.yet.talk.domain.usecase.post

import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.repository.PostRepository
import dev.zacsweers.metro.Inject

@Inject
class PublishPost(private val posts: PostRepository) {
    suspend operator fun invoke(content: String, images: List<Image> = emptyList(), quoteOf: PostId? = null): Result<PostId> =
        validated(content, images, isReply = false) { clean -> posts.publish(clean, images, quoteOf) }
}
