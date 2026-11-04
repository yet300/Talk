package com.yet.talk.domain.usecase

import com.yet.talk.domain.model.ImageFile
import com.yet.talk.domain.model.MediaUpload
import com.yet.talk.domain.repository.MediaRepository
import com.yet.talk.domain.rule.ContentRules
import dev.zacsweers.metro.Inject

@Inject
class UploadImage(private val media: MediaRepository) {
    suspend operator fun invoke(file: ImageFile): Result<MediaUpload> =
        ContentRules.validateImage(file).fold(onSuccess = { media.upload(it) }, onFailure = { Result.failure(it) })
}
