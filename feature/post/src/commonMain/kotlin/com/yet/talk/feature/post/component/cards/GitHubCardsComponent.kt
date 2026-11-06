package com.yet.talk.feature.post.component.cards

import com.yet.talk.domain.model.github.GitHubCard
import com.yet.talk.domain.model.github.GitHubRef
import com.yet.talk.domain.repository.GitHubRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Cards for GitHub links in posts (`RepoCard`), for every list that shows post cards. The platform UI collects
 * [card] for a post's [com.yet.talk.domain.model.post.PostSummary.github]; null shows the plain link row.
 */
interface GitHubCardsComponent {
    fun card(ref: GitHubRef): Flow<GitHubCard?>
}

/** For UI previews: no card, so the link row shows. */
class PreviewGitHubCardsComponent : GitHubCardsComponent {
    override fun card(ref: GitHubRef): Flow<GitHubCard?> = flowOf(null)
}

@Inject
internal class DefaultGitHubCardsComponent(private val github: GitHubRepository) : GitHubCardsComponent {
    override fun card(ref: GitHubRef): Flow<GitHubCard?> = github.card(ref)
}
