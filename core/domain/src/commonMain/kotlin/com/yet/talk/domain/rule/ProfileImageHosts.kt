package com.yet.talk.domain.rule

/**
 * Where avatars and banners may come from. Nostr cannot stop a profile from pointing anywhere (another client or
 * a script can set any URL), so Talk only sets links to sites that moderate what they host, and only shows
 * avatars and banners from them; anything else shows as the placeholder.
 */
object ProfileImageHosts {
    private const val GITHUB_AVATARS = "avatars.githubusercontent.com"

    /** Hosts a link pasted in the editor may point at. */
    val PASTEABLE: Set<String> = setOf("i.giphy.com", "media.giphy.com") + (0..4).map { "media$it.giphy.com" } + "images.unsplash.com"

    /** Hosts shown. GitHub avatars are set only from a verified GitHub link, never pasted. */
    val SHOWN: Set<String> = PASTEABLE + GITHUB_AVATARS

    /** True for a GitHub avatar link, which only a verified GitHub link may set. */
    fun isGitHubAvatar(url: String): Boolean = hostOf(url.trim()) == GITHUB_AVATARS

    /** [url] when it is an https link to a [SHOWN] host, else null. */
    fun shownOrNull(url: String?): String? = url?.takeIf { hostOf(it) in SHOWN }

    /**
     * The link as Talk stores it, or null when it is not an https link to a [PASTEABLE] host. A GIPHY page
     * (`giphy.com/gifs/<slug>-<id>` or `/stickers/`) becomes the link to its GIF.
     */
    fun normalize(link: String): String? {
        val url = link.trim()
        val host = hostOf(url) ?: return null
        if (host == "giphy.com" || host == "www.giphy.com") {
            val segments = url.pathSegments()
            if (segments.size != 2 || segments[0] !in setOf("gifs", "stickers")) return null
            val id = segments[1].substringAfterLast('-').takeIf { it.isNotEmpty() && it.all(Char::isLetterOrDigit) } ?: return null
            return "https://media.giphy.com/media/$id/giphy.gif"
        }
        return url.takeIf { host in PASTEABLE && url.pathSegments().isNotEmpty() }
    }

    /** The lower-case host of an `https://` URL without user info or port, else null. */
    private fun hostOf(url: String): String? {
        if (!url.startsWith("https://", ignoreCase = true)) return null
        val authority = url.substring("https://".length).substringBefore('/').substringBefore('?').substringBefore('#')
        if (authority.isEmpty() || '@' in authority || ':' in authority || authority.any(Char::isWhitespace)) return null
        return authority.lowercase()
    }

    private fun String.pathSegments(): List<String> =
        substring("https://".length).substringBefore('?').substringBefore('#').split('/').drop(1).filter(String::isNotEmpty)
}
