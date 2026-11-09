package com.yet.talk.mcp

import com.yet.talk.mcp.agent.AgentState
import com.yet.talk.mcp.agent.Budget
import com.yet.talk.mcp.agent.RateLimiter
import com.yet.talk.mcp.agent.ToolError
import com.yet.talk.mcp.config.RateLimit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class RateLimiterTest {
    private val store = MemoryStore()

    private fun limiter(perHour: Int = 2, gap: Long = 60) =
        RateLimiter(AgentState(store), mapOf(Budget.Posts to RateLimit(perHour, gap), Budget.Actions to RateLimit(100, 1)))

    @Test
    fun waitsOutTheGapBetweenPosts() = runTest {
        val limiter = limiter()
        assertEquals(0, limiter.retryAfter(Budget.Posts, 1_000))
        assertEquals(0, limiter.tryTake(Budget.Posts, 1_000))
        assertEquals(60, limiter.retryAfter(Budget.Posts, 1_000))
        assertEquals(10, limiter.retryAfter(Budget.Posts, 1_050))
        assertEquals(0, limiter.retryAfter(Budget.Posts, 1_060))
    }

    @Test
    fun capsPostsPerHourUntilTheOldestLeavesTheWindow() = runTest {
        val limiter = limiter()
        limiter.tryTake(Budget.Posts, 1_000)
        limiter.tryTake(Budget.Posts, 1_100)
        assertEquals(3_500, limiter.retryAfter(Budget.Posts, 1_100))
        assertEquals(0, limiter.remainingThisHour(Budget.Posts, 1_100))
        assertEquals(0, limiter.retryAfter(Budget.Posts, 4_600))
    }

    @Test
    fun limitsSurviveARestart() = runTest {
        limiter().tryTake(Budget.Posts, 1_000)
        assertEquals(60, limiter().retryAfter(Budget.Posts, 1_000))
    }

    @Test
    fun budgetsAreSeparate() = runTest {
        limiter().tryTake(Budget.Posts, 1_000)
        assertEquals(0, limiter().retryAfter(Budget.Actions, 1_000))
    }

    @Test
    fun aRefusedTakeRecordsNothing() = runTest {
        val limiter = limiter()
        limiter.tryTake(Budget.Posts, 1_000)
        assertEquals(30, limiter.tryTake(Budget.Posts, 1_030))
        assertEquals(1, 2 - limiter.remainingThisHour(Budget.Posts, 1_030))
    }

    @Test
    fun aDamagedStateFileStopsPublishing() {
        store.text = "{not json"
        assertFailsWith<ToolError> { limiter().retryAfter(Budget.Posts, 1_000) }
    }
}
