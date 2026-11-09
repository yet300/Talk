package com.yet.talk.mcp.agent

import com.yet.talk.mcp.config.RateLimit

/** Client-side limits, so that agents cannot flood public relays. Posts and everything else have separate budgets. */
internal class RateLimiter(private val state: AgentState, private val limits: Map<Budget, RateLimit>) {

    fun limit(budget: Budget): RateLimit = limits.getValue(budget)

    /** Seconds until [budget] allows one more event; 0 means now. */
    fun retryAfter(budget: Budget, now: Long): Long {
        val limit = limit(budget)
        val recent = state.times(budget).filter { it > now - HOUR }.sorted()
        val gapWait = recent.lastOrNull()?.let { it + limit.minGapSeconds - now } ?: 0
        val hourWait = if (recent.size >= limit.perHour) recent[recent.size - limit.perHour] + HOUR - now else 0
        return maxOf(gapWait, hourWait, 0)
    }

    fun remainingThisHour(budget: Budget, now: Long): Int =
        (limit(budget).perHour - state.times(budget).count { it > now - HOUR }).coerceAtLeast(0)

    fun take(budget: Budget, now: Long) = state.record(budget, now, forgetBefore = now - HOUR)

    private companion object {
        const val HOUR = 3600L
    }
}
