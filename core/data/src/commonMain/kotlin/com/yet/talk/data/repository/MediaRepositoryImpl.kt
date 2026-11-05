package com.yet.talk.data.repository

import com.yet.talk.api.source.media.MediaRemoteSource
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.remote
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.model.media.MediaUpload
import com.yet.talk.domain.repository.MediaRepository
import dev.zacsweers.metro.Inject

@Inject
internal class MediaRepositoryImpl(
    private val media: MediaRemoteSource,
    private val session: Session,
    private val publisher: Publisher,
) : MediaRepository {
    override suspend fun upload(file: ImageFile): Result<MediaUpload> = remote {
        val blob = media.upload(file.bytes, file.mimeType, session.requireSigner(), publisher.now())
        MediaUpload(url = blob.url, mimeType = blob.type ?: file.mimeType, sha256 = blob.sha256, sizeBytes = blob.size)
    }
}
