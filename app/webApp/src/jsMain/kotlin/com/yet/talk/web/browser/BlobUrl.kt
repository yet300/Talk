package com.yet.talk.web.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

/** A `blob:` URL for decrypted bytes (a chat image), revoked when it leaves the composition. */
@Composable
fun rememberBlobUrl(bytes: ByteArray, mimeType: String?): String {
    val url = remember(bytes) { URL.createObjectURL(Blob(arrayOf(bytes), BlobPropertyBag(type = mimeType ?: ""))) }
    DisposableEffect(url) { onDispose { URL.revokeObjectURL(url) } }
    return url
}
