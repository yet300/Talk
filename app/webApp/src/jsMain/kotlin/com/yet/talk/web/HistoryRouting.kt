package com.yet.talk.web

import kotlinx.browser.window
import org.w3c.dom.url.URL

/**
 * Returns an absolute path for an app route, prefixed with [BASE_PATH] when hosted at a subpath.
 */
internal fun appUrl(path: String): String {
    val prefix = BASE_PATH.trimEnd('/')
    return if (prefix.isEmpty()) {
        if (path.startsWith("/")) path else "/$path"
    } else {
        val cleanPath = path.trimStart('/')
        if (cleanPath.isEmpty()) "$prefix/" else "$prefix/$cleanPath"
    }
}

/**
 * When the app is served from a subpath (e.g. `/Talk/` on GitHub Pages),
 * intercepts `window.history.pushState` and `window.history.replaceState`
 * so root-based paths produced by Decompose (e.g. `/post/123`, `/`) are
 * prefixed with the repository subpath.
 */
internal fun installHistoryBasePath(basePath: String) {
    val prefix = basePath.trimEnd('/')
    if (prefix.isEmpty()) return

    val history = window.history.asDynamic()
    val rawPushState = history.pushState.bind(history)
    val rawReplaceState = history.replaceState.bind(history)

    fun normalizeUrl(url: dynamic): dynamic {
        if (url == null) return url
        val str = url.toString()
        return when {
            str == prefix || str.startsWith("$prefix/") -> str
            str.startsWith("/") -> "$prefix$str"
            else -> "$prefix/$str"
        }
    }

    history.pushState = { data: dynamic, title: dynamic, url: dynamic ->
        rawPushState(data, title, normalizeUrl(url))
    }
    history.replaceState = { data: dynamic, title: dynamic, url: dynamic ->
        rawReplaceState(data, title, normalizeUrl(url))
    }
}

/**
 * Strips the base path prefix from a deep link URL so the root component
 * can match the route against its known paths (`post/<id>`, `settings`, etc.).
 */
internal fun normalizeDeepLink(deepLink: String?, basePath: String): String? {
    if (deepLink == null) return null
    val prefix = basePath.trim('/')
    if (prefix.isEmpty()) return deepLink

    return try {
        val url = URL(deepLink)
        val pathname = url.pathname
        val normalizedPathname = if (pathname == "/$prefix" || pathname == "/$prefix/") {
            "/"
        } else if (pathname.startsWith("/$prefix/")) {
            pathname.removePrefix("/$prefix")
        } else {
            pathname
        }
        url.origin + normalizedPathname + url.search + url.hash
    } catch (_: Throwable) {
        if (deepLink.startsWith("/$prefix/")) {
            deepLink.removePrefix("/$prefix")
        } else if (deepLink == "/$prefix" || deepLink == "/$prefix/") {
            "/"
        } else {
            deepLink
        }
    }
}
