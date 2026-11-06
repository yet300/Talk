package com.yet.talk.domain.repository

import com.yet.talk.domain.model.github.GitHubAccount
import com.yet.talk.domain.model.github.GitHubCard
import com.yet.talk.domain.model.github.GitHubRef
import kotlinx.coroutines.flow.Flow

interface GitHubRepository {
    /** The card for a link, loaded once and cached; null while loading or when GitHub has none (a link row then). */
    fun card(ref: GitHubRef): Flow<GitHubCard?>

    /** The text the signed-in user puts in a public gist (NIP-39). */
    suspend fun proofText(): Result<String>

    /**
     * Checks that the gist at [gistLink] belongs to [login] and contains [proofText]. Fails with
     * [com.yet.talk.domain.model.error.DomainError.GitHubProofMissing] when it does not.
     */
    suspend fun verifyLink(login: String, gistLink: String): Result<GitHubAccount>
}
