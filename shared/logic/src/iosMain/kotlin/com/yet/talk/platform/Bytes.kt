package com.yet.talk.platform

import com.yet.talk.domain.model.media.ImageFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

/**
 * Bytes across the Swift bridge: an image picked in SwiftUI becomes an [ImageFile], a decrypted chat image becomes
 * `Data`. Swift: `BytesKt.imageFile(data:mimeType:)` and `BytesKt.toData(bytes)`.
 */
@OptIn(ExperimentalForeignApi::class)
fun imageFile(data: NSData, mimeType: String): ImageFile {
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    return ImageFile(bytes, mimeType)
}

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
fun toData(bytes: ByteArray): NSData =
    if (bytes.isEmpty()) NSData() else bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
