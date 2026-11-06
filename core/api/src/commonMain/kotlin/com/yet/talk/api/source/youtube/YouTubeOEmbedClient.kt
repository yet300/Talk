package com.yet.talk.api.source.youtube

import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Inject
internal class YouTubeOEmbedClient(private val http: HttpClient) : YouTubeRemoteSource {

    override suspend fun video(videoId: String): YouTubeOEmbed? {
        val response = http.get("https://www.youtube.com/oembed") {
            parameter("url", "https://www.youtube.com/watch?v=$videoId")
            parameter("format", "json")
        }
        return when {
            response.status.value in 400..499 -> null // 404 missing, 401 private or not embeddable
            !response.status.isSuccess() -> throw IllegalStateException("YouTube answered ${response.status.value}")
            else -> JSON.decodeFromString(OEmbed.serializer(), response.bodyAsText()).let { YouTubeOEmbed(it.title, it.authorName) }
        }
    }

    @Serializable
    private class OEmbed(val title: String = "", @SerialName("author_name") val authorName: String = "")

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
