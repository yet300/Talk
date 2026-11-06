package com.yet.talk.api.source.github

import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Inject
internal class GitHubClient(private val http: HttpClient) : GitHubRemoteSource {

    override suspend fun gist(id: String): GistDto? = get("gists/${id.encodeURLPathPart()}", Gist.serializer())?.let { gist ->
        GistDto(gist.owner?.login ?: return null, gist.owner.avatarUrl, gist.files.values.joinToString("\n") { it.content.orEmpty() })
    }

    override suspend fun repo(owner: String, name: String): RepoDto? =
        get("repos/${owner.encodeURLPathPart()}/${name.encodeURLPathPart()}", Repo.serializer())?.let {
            RepoDto(it.fullName, it.description, it.language, it.stars, it.forks, it.owner?.avatarUrl)
        }

    override suspend fun user(login: String): UserDto? = get("users/${login.encodeURLPathPart()}", User.serializer())?.let {
        UserDto(it.login, it.name, it.bio, it.followers, it.avatarUrl)
    }

    private suspend fun <T> get(path: String, serializer: kotlinx.serialization.KSerializer<T>): T? {
        val response: HttpResponse = http.get("$API/$path") { header("Accept", "application/vnd.github+json") }
        return when {
            response.status == HttpStatusCode.NotFound -> null
            !response.status.isSuccess() -> throw IllegalStateException("GitHub answered ${response.status.value}")
            else -> JSON.decodeFromString(serializer, response.bodyAsText())
        }
    }

    @Serializable
    private class Owner(val login: String, @SerialName("avatar_url") val avatarUrl: String? = null)

    @Serializable
    private class GistFile(val content: String? = null)

    @Serializable
    private class Gist(val owner: Owner? = null, val files: Map<String, GistFile> = emptyMap())

    @Serializable
    private class Repo(
        @SerialName("full_name") val fullName: String,
        val description: String? = null,
        val language: String? = null,
        @SerialName("stargazers_count") val stars: Long = 0,
        @SerialName("forks_count") val forks: Long = 0,
        val owner: Owner? = null,
    )

    @Serializable
    private class User(
        val login: String,
        val name: String? = null,
        val bio: String? = null,
        val followers: Long = 0,
        @SerialName("avatar_url") val avatarUrl: String? = null,
    )

    private companion object {
        const val API = "https://api.github.com"
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
