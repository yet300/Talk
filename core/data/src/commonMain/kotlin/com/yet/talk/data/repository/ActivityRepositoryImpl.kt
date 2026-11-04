package com.yet.talk.data.repository

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.source.post.PostRemoteSource
import com.yet.talk.api.source.social.SocialRemoteSource
import com.yet.talk.data.mapper.toPost
import com.yet.talk.data.mapper.userOf
import com.yet.talk.data.paging.FeedPager
import com.yet.talk.data.paging.FetchedPage
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.dao.ProfileDao
import com.yet.talk.database.dao.SocialDao
import com.yet.talk.database.model.ActivityRecord
import com.yet.talk.domain.model.Pager
import com.yet.talk.domain.model.activity.ActivityItem
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.repository.ActivityRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

@OptIn(ExperimentalCoroutinesApi::class)
@ContributesBinding(AppScope::class)
@Inject
class ActivityRepositoryImpl(
    private val posts: PostDao,
    private val profiles: ProfileDao,
    private val social: SocialDao,
    private val postsRemote: PostRemoteSource,
    private val socialRemote: SocialRemoteSource,
    private val sync: RemoteSync,
    private val ingestor: EventIngestor,
    private val session: Session,
) : ActivityRepository {

    override fun activity(): Pager<ActivityItem> = FeedPager(
        observe = { limit ->
            session.viewer.flatMapLatest { viewer ->
                if (viewer.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    social.observeActivity(viewer, limit).mapLatest { records -> records.mapNotNull { it.toItem(viewer) } }
                }
            }
        },
        fetch = { until -> fetch(until) },
    )

    private suspend fun fetch(until: Long?): FetchedPage {
        val me = session.viewer.first()
        if (me.isEmpty()) return FetchedPage(0, null)
        val events = socialRemote.activity(me, until, Pager.PAGE_SIZE)
        if (until == null) ingestor.ingest(socialRemote.followers(me, FOLLOWERS_LIMIT))
        // The viewer's own posts the activity points at, so they can be shown.
        val targets = events.mapNotNull {
            when (it) {
                is TalkEvent.Reaction -> it.postId
                is TalkEvent.Post -> it.thread.parent ?: it.quoteId
                else -> null
            }
        }.distinct()
        sync.storeWithContext(postsRemote.byIds(targets))
        val replies = events.filterIsInstance<TalkEvent.Post>()
        val newItems = sync.storeWithContext(replies) + ingestor.ingest(events - replies.toSet())
        ingestor.ingest(socialRemote.profiles(events.map { it.event.pubkey }.toSet()))
        return FetchedPage(newItems, events.minOfOrNull { it.event.createdAt })
    }

    private suspend fun ActivityRecord.toItem(viewer: String): ActivityItem? {
        val type = when (type) {
            "reply" -> ActivityItem.Type.Reply
            "quote" -> ActivityItem.Type.Quote
            "repost" -> ActivityItem.Type.Repost
            "like" -> ActivityItem.Type.Like
            "follow" -> ActivityItem.Type.Follow
            else -> return null
        }
        return ActivityItem(
            type = type,
            actor = userOf(actor, profiles.observe(actor).first()),
            createdAt = Instant.fromEpochSeconds(createdAt),
            eventPost = eventPostId?.let { posts.observePost(it, viewer).first()?.toPost() },
            subjectPost = subjectPostId?.let { posts.observePost(it, viewer).first()?.toPost() },
        )
    }

    private companion object {
        const val FOLLOWERS_LIMIT = 100
    }
}
