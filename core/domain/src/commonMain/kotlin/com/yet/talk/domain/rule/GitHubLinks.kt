package com.yet.talk.domain.rule

import com.yet.talk.domain.model.github.GitHubRef

/** GitHub links in posts and gist links in the profile editor. */
object GitHubLinks {
    /** Top-level github.com paths that are pages, not users. */
    private val RESERVED = setOf(
        "about", "apps", "collections", "contact", "customer-stories", "enterprise", "events", "explore", "features",
        "issues", "join", "login", "logout", "marketplace", "new", "notifications", "orgs", "organizations", "pricing",
        "pulls", "search", "security", "settings", "site", "sponsors", "topics", "trending",
    )
    private val URL = Regex("""https?://(?:www\.)?github\.com/([A-Za-z0-9-]{1,39})(?:/([A-Za-z0-9._-]{1,100}))?/?(?=[\s)\]>"'.,;:!?]|$)""")
    private val GIST = Regex("""^https://gist\.github\.com/(?:[A-Za-z0-9-]{1,39}/)?([0-9a-fA-F]{20,64})/?(?:[?#].*)?$""")
    private val LOGIN = Regex("^[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38}$")

    /** The first link to a GitHub user or repository outside code blocks, or null. */
    fun find(markdown: String): GitHubRef? {
        var inCode = false
        for (line in markdown.lineSequence()) {
            if (line.trimStart().startsWith("```")) {
                inCode = !inCode
                continue
            }
            if (inCode) continue
            for (match in URL.findAll(line.replace(Regex("`[^`]*`"), ""))) {
                val owner = match.groupValues[1]
                val repo = match.groupValues[2].removeSuffix(".git")
                if (owner.lowercase() in RESERVED || !isLogin(owner)) continue
                return if (repo.isEmpty()) GitHubRef.User(owner) else GitHubRef.Repo(owner, repo)
            }
        }
        return null
    }

    /** The gist id in `https://gist.github.com/<login>/<id>`, or null. */
    fun gistId(link: String): String? = GIST.find(link.trim())?.groupValues?.get(1)?.lowercase()

    fun isLogin(text: String): Boolean = LOGIN.matches(text)
}
