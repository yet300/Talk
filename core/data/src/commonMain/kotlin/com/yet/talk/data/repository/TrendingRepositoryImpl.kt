package com.yet.talk.data.repository

import androidx.paging.PagingData
import androidx.paging.map
import com.yet.talk.api.source.post.PostRemoteSource
import com.yet.talk.data.mapper.toPost
import com.yet.talk.data.paging.FeedPaging
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.dao.TrendingDao
import com.yet.talk.database.model.PostFeed
import com.yet.talk.database.model.PostRecord
import com.yet.talk.database.model.ReactionTypes
import com.yet.talk.database.model.TrendingRecord
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.TrendingRepository
import com.yet.talk.domain.rule.TrendingRanker
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
internal class TrendingRepositoryImpl(
    private val posts: PostDao,
    private val trending: TrendingDao,
    private val postsRemote: PostRemoteSource,
    private val sync: RemoteSync,
    private val session: Session,
    private val publisher: Publisher,
    private val paging: FeedPaging,
) : TrendingRepository {

    // Ranked locally: refresh rebuilds the table; later pages come from the table, not from relays.
    override fun trending(): Flow<PagingData<Post>> =
        session.viewer.flatMapLatest { viewer ->
            paging.feed(
                feedKey = "trending",
                pages = { posts.feedPages(PostFeed.Trending, viewer) },
                fetcher = { until -> if (until == null) rebuild() else null },
            )
        }.map { page -> page.map(PostRecord::toPost) }

    /** Rebuilds the ranking from the last 7 days at most every 3 minutes, like KarpathyTalk's server did. */
    private suspend fun rebuild(): Long? {
        val now = publisher.now()
        val computedAt = trending.computedAt()
        if (computedAt != null && now - computedAt < TrendingRanker.REBUILD_INTERVAL.inWholeSeconds) return now
        val since = now - TrendingRanker.WINDOW.inWholeSeconds
        sync.storeWithContext(postsRemote.since(since, limit = WINDOW_FETCH_LIMIT))

        val candidates = trending.candidates(since).map {
            TrendingRanker.Candidate(PostId(it.postId), UserId(it.author), Instant.fromEpochSeconds(it.createdAt))
        }
        val engagements = trending.engagements(since).mapNotNull {
            val type = when (it.type) {
                ENGAGEMENT_REPLY -> TrendingRanker.Engagement.Type.Reply
                ReactionTypes.LIKE -> TrendingRanker.Engagement.Type.Like
                ReactionTypes.REPOST -> TrendingRanker.Engagement.Type.Repost
                else -> return@mapNotNull null
            }
            TrendingRanker.Engagement(PostId(it.postId), UserId(it.actor), type)
        }
        val ranked = TrendingRanker.rank(candidates, engagements, Instant.fromEpochSeconds(now))
        trending.replace(ranked.map { TrendingRecord(it.rank, it.postId.value, it.score) }, computedAt = now)
        return now
    }

    private companion object {
        const val WINDOW_FETCH_LIMIT = 500
        const val ENGAGEMENT_REPLY = "reply"
    }
}
