package com.yet.talk.domain.usecase.post

import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.rule.ContentRules

internal inline fun <T> validated(
    content: String,
    images: List<Image>,
    isReply: Boolean,
    write: (String) -> Result<T>,
): Result<T> {
    ContentRules.validateImageCount(images.size).onFailure { return Result.failure(it) }
    return ContentRules.validatePost(content, isReply).fold(onSuccess = write, onFailure = { Result.failure(it) })
}
