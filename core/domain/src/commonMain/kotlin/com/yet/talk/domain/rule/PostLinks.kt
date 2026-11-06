package com.yet.talk.domain.rule

import com.yet.talk.domain.model.post.PostLink

/** Finds the link a post card shows: the first YouTube video, else the first GitHub repository or user, else the first link. */
object PostLinks {
    private val URL = Regex("""https?://[^\s<>()\[\]"'`]+""")
    // JS regexes run in unicode mode, where a lone `]` is an error: it is escaped.
    private val IMAGE = Regex("""!\[[^\]]*\]\([^)]*\)""")
    private val YOUTUBE_ID = Regex("^[A-Za-z0-9_-]{11}$")

    fun find(markdown: String): PostLink? {
        val urls = linksOutsideCode(markdown).toList()
        urls.firstNotNullOfOrNull { url -> youTubeId(url)?.let { PostLink.YouTube(it, url) } }?.let { return it }
        GitHubLinks.find(markdown)?.let { return PostLink.GitHub(it) }
        return urls.firstOrNull()?.let(PostLink::Web)
    }

    /** The video id of a youtube.com/watch, youtu.be, /shorts/, /embed/ or /live/ link. */
    fun youTubeId(url: String): String? {
        val withoutScheme = url.substringAfter("://")
        val host = withoutScheme.substringBefore('/').substringBefore('?').lowercase().removePrefix("www.").removePrefix("m.")
        val path = withoutScheme.substringAfter('/', "").substringBefore('#')
        val id = when (host) {
            "youtu.be" -> path.substringBefore('?').substringBefore('/')
            "youtube.com", "youtube-nocookie.com" -> when {
                path.startsWith("watch") -> path.substringAfter('?', "").split('&').firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
                else -> path.substringBefore('?').split('/').let { parts ->
                    parts.getOrNull(1)?.takeIf { parts.firstOrNull() in setOf("shorts", "embed", "live") }
                }
            }
            else -> null
        }
        return id?.takeIf { YOUTUBE_ID.matches(it) }
    }

    private fun linksOutsideCode(markdown: String): Sequence<String> = sequence {
        var inCode = false
        for (line in markdown.lineSequence()) {
            if (line.trimStart().startsWith("```")) {
                inCode = !inCode
                continue
            }
            if (inCode) continue
            val text = line.replace(IMAGE, "").replace(Regex("`[^`]*`"), "")
            for (match in URL.findAll(text)) yield(match.value.trimEnd('.', ',', ';', ':', '!', '?'))
        }
    }
}
