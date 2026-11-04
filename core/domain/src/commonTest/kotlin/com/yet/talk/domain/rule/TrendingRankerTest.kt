package com.yet.talk.domain.rule

import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.rule.TrendingRanker.Candidate
import com.yet.talk.domain.rule.TrendingRanker.Engagement
import com.yet.talk.domain.rule.TrendingRanker.Engagement.Type
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** Ported from `trending_test.go`. */
class TrendingRankerTest {

    private val now = Instant.fromEpochSeconds(1_800_000_000)
    private val author = UserId("author")
    private val other = UserId("other")

    private fun post(id: String, age: Duration = 0.hours) = Candidate(PostId(id), author, now - age)

    private fun engage(id: String, count: Int, type: Type = Type.Reply, actor: UserId = other) =
        List(count) { Engagement(PostId(id), actor, type) }

    @Test
    fun excludesZeroEngagement() {
        val ranked = TrendingRanker.rank(listOf(post("a"), post("b"), post("c")), engage("b", 1), now)
        assertEquals(listOf(PostId("b")), ranked.map { it.postId })
    }

    @Test
    fun ordersByEngagementWhenAgesAreEqual() {
        val ranked = TrendingRanker.rank(
            listOf(post("a"), post("b"), post("c")),
            engage("a", 1) + engage("b", 10) + engage("c", 5),
            now,
        )
        assertEquals(listOf("b", "c", "a"), ranked.map { it.postId.value })
        assertEquals(listOf(1, 2, 3), ranked.map { it.rank })
    }

    @Test
    fun ageDecayBeatsRawEngagement() {
        val ranked = TrendingRanker.rank(listOf(post("old", 100.hours), post("fresh")), engage("old", 10) + engage("fresh", 2), now)
        assertEquals(listOf("fresh", "old"), ranked.map { it.postId.value })
        assertTrue(abs(ranked[0].score - 2 / 2.0.pow(1.8)) < 1e-9)
        assertTrue(abs(ranked[1].score - 10 / 102.0.pow(1.8)) < 1e-9)
    }

    @Test
    fun excludesPostsOutsideTheWindow() {
        val ranked = TrendingRanker.rank(listOf(post("fresh"), post("old", 8.days)), engage("fresh", 1) + engage("old", 50), now)
        assertEquals(listOf("fresh"), ranked.map { it.postId.value })
    }

    @Test
    fun excludesSelfEngagementAndCountsEachLikeOnce() {
        val ranked = TrendingRanker.rank(
            listOf(post("real"), post("gamed")),
            engage("real", 5) +
                engage("gamed", 5, actor = author) + engage("gamed", 1, Type.Like, author) + engage("gamed", 1, Type.Repost, author) +
                engage("gamed", 3, Type.Like),
            now,
        )
        assertEquals(listOf("real", "gamed"), ranked.map { it.postId.value })
        assertTrue(abs(ranked[1].score - 1 / 2.0.pow(1.8)) < 1e-9, "three likes by one user count once")
    }

    @Test
    fun ignoresEngagementOnUnknownPostsAndKeepsTopN() {
        val candidates = List(250) { post("p$it") }
        val engagements = candidates.flatMap { engage(it.postId.value, 1) } + engage("reply-not-a-candidate", 9)
        val ranked = TrendingRanker.rank(candidates, engagements, now)
        assertEquals(TrendingRanker.TOP_N, ranked.size)
        assertEquals((1..TrendingRanker.TOP_N).toList(), ranked.map { it.rank })
    }
}
