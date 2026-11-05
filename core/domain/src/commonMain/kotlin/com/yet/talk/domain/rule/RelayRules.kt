package com.yet.talk.domain.rule

import com.yet.talk.domain.model.error.DomainError

/** Relay addresses as users type them. */
object RelayRules {
    /**
     * Trims and lower-cases the scheme and host, drops a trailing slash, and accepts only `wss://`
     * (and `ws://` for a local relay), so the same relay is not listed twice.
     */
    fun normalize(input: String): Result<String> {
        val url = input.trim().trimEnd('/')
        val scheme = url.substringBefore("://", missingDelimiterValue = "").lowercase()
        val rest = url.substringAfter("://", missingDelimiterValue = "")
        val host = rest.substringBefore('/')
        val valid = (scheme == "wss" || (scheme == "ws" && host.substringBefore(':') in LOCAL_HOSTS)) &&
            host.isNotEmpty() && host.none { it.isWhitespace() }
        if (!valid) return Result.failure(DomainError.InvalidRelayUrl)
        return Result.success("$scheme://${host.lowercase()}" + rest.removePrefix(host))
    }

    private val LOCAL_HOSTS = setOf("localhost", "127.0.0.1")
}
