package com.yet.talk.mcp.agent

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Every write passes through here, one at a time: dry run first, then the rate limit, then the AI disclosure. */
internal class PublishGate(
    val publishEnabled: Boolean,
    private val limiter: RateLimiter,
    private val identity: AgentIdentity,
    private val now: () -> Long,
) {
    private val lock = Mutex()

    /**
     * Runs [publish] when real publishing is on; otherwise reports [preview] and sends nothing. A failed attempt still
     * uses up its budget, so an agent retrying on an error cannot flood relays.
     */
    suspend fun run(
        budget: Budget,
        preview: JsonObject,
        requireDisclosure: Boolean = true,
        publish: suspend () -> JsonObject,
    ): JsonObject = lock.withLock {
        val wait = limiter.retryAfter(budget, now())
        if (!publishEnabled) return@withLock dryRun(preview, wait)
        if (wait > 0) throw ToolError("Rate limit: nothing was published. Try again in $wait seconds.")
        if (requireDisclosure) identity.ensureDisclosed()
        limiter.take(budget, now())
        publish()
    }

    private fun dryRun(preview: JsonObject, wait: Long) = buildJsonObject {
        put("dry_run", true)
        put("published", false)
        put("message", "Dry run: nothing was sent. The server's operator turns on publishing with TALK_MCP_PUBLISH=true.")
        put("retry_after_seconds", JsonPrimitive(wait))
        put("would_publish", preview)
    }
}
