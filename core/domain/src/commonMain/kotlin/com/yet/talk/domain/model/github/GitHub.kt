package com.yet.talk.domain.model.github

/** A GitHub account linked to a Nostr key by a gist (NIP-39); only verified links are shown. */
data class GitHubAccount(val login: String, val gistId: String) {
    /** GitHub serves the account's current avatar at this address. */
    val avatarUrl: String get() = "https://avatars.githubusercontent.com/$login"
}

/** A link to GitHub found in a post. */
sealed interface GitHubRef {
    val url: String

    data class Repo(val owner: String, val name: String) : GitHubRef {
        override val url get() = "https://github.com/$owner/$name"
    }

    data class User(val login: String) : GitHubRef {
        override val url get() = "https://github.com/$login"
    }
}

/** What a [GitHubRef] card shows. */
sealed interface GitHubCard {
    val ref: GitHubRef
    val avatarUrl: String?

    data class Repo(
        override val ref: GitHubRef.Repo,
        val fullName: String,
        val description: String?,
        val language: String?,
        val stars: Long,
        val forks: Long,
        override val avatarUrl: String?,
    ) : GitHubCard

    data class User(
        override val ref: GitHubRef.User,
        val name: String?,
        val bio: String?,
        val followers: Long,
        override val avatarUrl: String?,
    ) : GitHubCard
}
