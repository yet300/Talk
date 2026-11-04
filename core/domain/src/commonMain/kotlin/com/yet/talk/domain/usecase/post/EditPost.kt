package com.yet.talk.domain.usecase.post

import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.repository.PostRepository
import dev.zacsweers.metro.Inject

/** Edits keep the post's kind: a reply stays within the reply limit. */
@Inject
class EditPost(private val posts: PostRepository) {
    suspend operator fun invoke(id: PostId, content: String, images: List<Image>, isReply: Boolean): Result<Unit> =
        validated(content, images, isReply) { clean -> posts.edit(id, clean, images) }
}
