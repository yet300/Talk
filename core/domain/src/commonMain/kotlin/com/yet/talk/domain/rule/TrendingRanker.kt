package com.yet.talk.domain.rule

import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import kotlin.math.pow
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * KarpathyTalk's trending formula (`trending.go`), Hacker News style:
 * `score = engagement / (age_hours + 2)^1.8`, over top-level posts from the last 7 days.
 * Engagement counts replies, likes and reposts by anyone but the author; a user's like or repost
 * counts once, every reply counts. Posts without engagement are not ranked.
 */
object TrendingRanker {
    val WINDOW = 7.days
    val REBUILD_INTERVAL = 3.minutes
    const val TOP_N = 200
    const val DECAY_EXPONENT = 1.8

    data class Candidate(val postId: PostId, val authorId: UserId, val createdAt: Instant)

    data class Engagement(val postId: PostId, val actorId: UserId, val type: Type) {
        enum class Type { Reply, Like, Repost }
    }

    data class Ranked(val postId: PostId, val rank: Int, val score: Double)

    fun rank(candidates: List<Candidate>, engagements: List<Engagement>, now: Instant): List<Ranked> {
        val cutoff = now - WINDOW
        val eligible = candidates.filter { it.createdAt > cutoff }.associateBy { it.postId }
        val counted = engagements
            .filter { engagement -> eligible[engagement.postId]?.let { it.authorId != engagement.actorId } == true }
            .let { list -> list.filter { it.type == Engagement.Type.Reply } + list.filter { it.type != Engagement.Type.Reply }.distinct() }
            .groupingBy { it.postId }
            .eachCount()

        return counted
            .map { (postId, engagement) ->
                val ageHours = ((now - eligible.getValue(postId).createdAt).inWholeSeconds / 3600.0).coerceAtLeast(0.0)
                postId to engagement / (ageHours + 2).pow(DECAY_EXPONENT)
            }
            .sortedByDescending { it.second }
            .take(TOP_N)
            .mapIndexed { index, (postId, score) -> Ranked(postId, rank = index + 1, score = score) }
    }
}
