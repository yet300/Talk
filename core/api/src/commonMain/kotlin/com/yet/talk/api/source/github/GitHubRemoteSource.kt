package com.yet.talk.api.source.github

/**
 * GitHub's REST API, read without a token (60 requests an hour per IP, which is why callers cache). Every call
 * returns null when GitHub has no such thing (404) and throws when it cannot answer (network, rate limit).
 */
interface GitHubRemoteSource {
    suspend fun gist(id: String): GistDto?

    suspend fun repo(owner: String, name: String): RepoDto?

    suspend fun user(login: String): UserDto?
}

data class GistDto(val ownerLogin: String, val ownerAvatarUrl: String?, val text: String)

data class RepoDto(
    val fullName: String,
    val description: String?,
    val language: String?,
    val stars: Long,
    val forks: Long,
    val ownerAvatarUrl: String?,
)

data class UserDto(val login: String, val name: String?, val bio: String?, val followers: Long, val avatarUrl: String?)
