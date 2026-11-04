package com.yet.talk.domain.usecase

import com.yet.talk.domain.model.Image
import com.yet.talk.domain.model.PostId
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.rule.ContentRules
import dev.zacsweers.metro.Inject

@Inject
class PublishPost(private val posts: PostRepository) {
    suspend operator fun invoke(content: String, images: List<Image> = emptyList(), quoteOf: PostId? = null): Result<PostId> =
        validated(content, images, isReply = false) { clean -> posts.publish(clean, images, quoteOf) }
}

@Inject
class ReplyToPost(private val posts: PostRepository) {
    suspend operator fun invoke(parent: PostId, content: String, images: List<Image> = emptyList()): Result<PostId> =
        validated(content, images, isReply = true) { clean -> posts.reply(parent, clean, images) }
}

/** Edits keep the post's kind: a reply stays within the reply limit. */
@Inject
class EditPost(private val posts: PostRepository) {
    suspend operator fun invoke(id: PostId, content: String, images: List<Image>, isReply: Boolean): Result<Unit> =
        validated(content, images, isReply) { clean -> posts.edit(id, clean, images) }
}

private inline fun <T> validated(
    content: String,
    images: List<Image>,
    isReply: Boolean,
    write: (String) -> Result<T>,
): Result<T> {
    ContentRules.validateImageCount(images.size).onFailure { return Result.failure(it) }
    return ContentRules.validatePost(content, isReply).fold(onSuccess = write, onFailure = { Result.failure(it) })
}
