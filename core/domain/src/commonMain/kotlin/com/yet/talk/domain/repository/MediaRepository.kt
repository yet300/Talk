package com.yet.talk.domain.repository

import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.model.media.MediaUpload

interface MediaRepository {
    suspend fun upload(file: ImageFile): Result<MediaUpload>
}
