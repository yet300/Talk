package com.yet.talk.api.source.media

import com.yet.talk.nostr.signer.Signer

interface MediaRemoteSource {
    /** Uploads to Blossom, authorised by a kind 24242 event signed by [signer]. */
    suspend fun upload(bytes: ByteArray, mimeType: String, signer: Signer, createdAt: Long): BlobDescriptor

    /** Downloads a blob (an encrypted chat file); the caller checks its hash. */
    suspend fun download(url: String): ByteArray
}
