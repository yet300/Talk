package com.yet.talk.data.repository

import com.yet.talk.api.source.post.PostRemoteSource
import com.yet.talk.data.mapper.toPost
import com.yet.talk.data.paging.FeedPager
import com.yet.talk.data.paging.FetchedPage
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.dao.TrendingDao
import com.yet.talk.database.model.PostFeed
import com.yet.talk.database.model.PostRecord
import com.yet.talk.database.model.TrendingRecord
import com.yet.talk.domain.model.Pager
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.TrendingRepository
import com.yet.talk.domain.rule.TrendingRanker
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@ContributesBinding(AppScope::class)
@Inject
class TrendingRepositoryImpl(
    private val posts: PostDao,
    private val trending: TrendingDao,
    private val postsRemote: PostRemoteSource,
    private val sync: RemoteSync,
    private val session: Session,
    private val publisher: Publisher,
) : TrendingRepository {

    override fun trending(): Pager<Post> = FeedPager(
        observe = { limit ->
            session.viewer.flatMapLatest { viewer -> posts.observeFeed(PostFeed.Trending, viewer, limit) }
                .map { records -> records.map(PostRecord::toPost) }
        },
        // Ranked locally: later pages come from the table, not from relays.
        fetch = { until -> if (until != null) FetchedPage(0, null) else FetchedPage(rebuild(), null) },
    )

    /** Rebuilds the ranking from the last 7 days at most every 3 minutes, like KarpathyTalk's server did. */
    private suspend fun rebuild(): Int {
        val now = publisher.now()
        val computedAt = trending.computedAt()
        if (computedAt != null && now - computedAt < TrendingRanker.REBUILD_INTERVAL.inWholeSeconds) return 0
        val since = now - TrendingRanker.WINDOW.inWholeSeconds
        sync.storeWithContext(postsRemote.since(since, limit = WINDOW_FETCH_LIMIT))

        val candidates = trending.candidates(since).map {
            TrendingRanker.Candidate(PostId(it.postId), UserId(it.author), Instant.fromEpochSeconds(it.createdAt))
        }
        val engagements = trending.engagements(since).mapNotNull {
            val type = when (it.type) {
                "reply" -> TrendingRanker.Engagement.Type.Reply
                "like" -> TrendingRanker.Engagement.Type.Like
                "repost" -> TrendingRanker.Engagement.Type.Repost
                else -> return@mapNotNull null
            }
            TrendingRanker.Engagement(PostId(it.postId), UserId(it.actor), type)
        }
        val ranked = TrendingRanker.rank(candidates, engagements, Instant.fromEpochSeconds(now))
        trending.replace(ranked.map { TrendingRecord(it.rank, it.postId.value, it.score) }, computedAt = now)
        return ranked.size
    }

    private companion object {
        const val WINDOW_FETCH_LIMIT = 500
    }
}
