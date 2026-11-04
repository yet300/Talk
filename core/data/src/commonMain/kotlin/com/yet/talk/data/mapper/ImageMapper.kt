package com.yet.talk.data.mapper

import com.yet.talk.domain.model.post.Image
import com.yet.talk.nostr.nip.Imeta
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Images are stored as JSON in a TEXT column. */
@Serializable
internal data class StoredImage(
    val url: String,
    val mimeType: String? = null,
    val sha256: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val alt: String? = null,
)

private val json = Json { ignoreUnknownKeys = true }
private val imagesSerializer = ListSerializer(StoredImage.serializer())

internal fun List<Imeta>.toImagesJson(): String =
    json.encodeToString(imagesSerializer, map { StoredImage(it.url, it.mimeType, it.sha256, it.width, it.height, it.alt) })

internal fun List<Image>.toImeta(): List<Imeta> = map { Imeta(it.url, it.mimeType, width = it.width, height = it.height, alt = it.alt) }

internal fun imagesFromJson(value: String): List<Image> =
    runCatching { json.decodeFromString(imagesSerializer, value) }.getOrDefault(emptyList())
        .map { Image(it.url, it.mimeType, it.width, it.height, it.alt) }
