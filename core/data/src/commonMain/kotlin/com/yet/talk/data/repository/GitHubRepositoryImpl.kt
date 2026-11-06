package com.yet.talk.data.repository

import com.yet.talk.api.source.github.GitHubRemoteSource
import com.yet.talk.data.github.GitHubProofs
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.GitHubCardDao
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.github.GitHubAccount
import com.yet.talk.domain.model.github.GitHubCard
import com.yet.talk.domain.model.github.GitHubRef
import com.yet.talk.domain.repository.GitHubRepository
import com.yet.talk.domain.rule.GitHubLinks
import com.yet.talk.nostr.encoding.Nip19
import com.yet.talk.nostr.nip.githubProofText
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Cards come from the database while fresh (a day for a card, an hour for "GitHub has none"). GitHub allows 60
 * requests an hour without a token, so after a failure no card is fetched for a few minutes, and only one
 * request runs at a time.
 */
@SingleIn(AppScope::class)
@Inject
internal class GitHubRepositoryImpl(
    private val github: GitHubRemoteSource,
    private val cards: GitHubCardDao,
    private val proofs: GitHubProofs,
    private val publisher: Publisher,
) : GitHubRepository {
    private val fetching = Mutex()
    private var pausedUntil = 0L

    override fun card(ref: GitHubRef): Flow<GitHubCard?> = flow {
        val key = ref.key()
        val cached = cards.get(key)
        cached?.first?.let { emit(decode(ref, it)) }
        if (cached != null && publisher.now() - cached.second < (if (cached.first == null) MISSING_TTL else CARD_TTL)) {
            if (cached.first == null) emit(null)
            return@flow
        }
        val json = fetching.withLock {
            // Another collector may have fetched it while this one waited.
            cards.get(key)?.takeIf { it != cached }?.let { return@withLock it.first }
            if (publisher.now() < pausedUntil) return@withLock cached?.first
            runCatching { fetch(ref) }
                .onSuccess { cards.put(key, it, publisher.now()) }
                .getOrElse {
                    pausedUntil = publisher.now() + PAUSE_AFTER_FAILURE
                    cached?.first
                }
        }
        emit(json?.let { decode(ref, it) })
    }

    override suspend fun proofText(): Result<String> = remote { githubProofText(Nip19.npub(publisher.me())) }

    override suspend fun verifyLink(login: String, gistLink: String): Result<GitHubAccount> {
        val name = login.trim().removePrefix("@")
        if (!GitHubLinks.isLogin(name)) return Result.failure(DomainError.InvalidGitHubLogin)
        val gistId = GitHubLinks.gistId(gistLink) ?: return Result.failure(DomainError.InvalidGistLink)
        return remote {
            val gist = proofs.check(publisher.me(), name, gistId) ?: throw DomainError.GitHubProofMissing
            GitHubAccount(gist.ownerLogin, gistId)
        }
    }

    private suspend fun fetch(ref: GitHubRef): String? = when (ref) {
        is GitHubRef.Repo -> github.repo(ref.owner, ref.name)?.let {
            JSON.encodeToString(Stored.serializer(), Stored(it.fullName, it.description, it.language, it.stars, it.forks, it.ownerAvatarUrl))
        }
        is GitHubRef.User -> github.user(ref.login)?.let {
            JSON.encodeToString(Stored.serializer(), Stored(it.login, it.bio, null, 0, 0, it.avatarUrl, name = it.name, followers = it.followers))
        }
    }

    private fun decode(ref: GitHubRef, json: String): GitHubCard? = runCatching { JSON.decodeFromString(Stored.serializer(), json) }
        .getOrNull()
        ?.let { stored ->
            when (ref) {
                is GitHubRef.Repo -> GitHubCard.Repo(ref, stored.title, stored.text, stored.language, stored.stars, stored.forks, stored.avatarUrl)
                is GitHubRef.User -> GitHubCard.User(ref, stored.name, stored.text, stored.followers, stored.avatarUrl)
            }
        }

    private fun GitHubRef.key(): String = when (this) {
        is GitHubRef.Repo -> "repo:${owner.lowercase()}/${name.lowercase()}"
        is GitHubRef.User -> "user:${login.lowercase()}"
    }

    /** One stored shape for both cards: [title] is the repository's full name or the login. */
    @Serializable
    private class Stored(
        val title: String,
        val text: String?,
        val language: String?,
        val stars: Long,
        val forks: Long,
        val avatarUrl: String?,
        val name: String? = null,
        val followers: Long = 0,
    )

    private companion object {
        const val CARD_TTL = 24 * 60 * 60L
        const val MISSING_TTL = 60 * 60L
        const val PAUSE_AFTER_FAILURE = 5 * 60L
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
