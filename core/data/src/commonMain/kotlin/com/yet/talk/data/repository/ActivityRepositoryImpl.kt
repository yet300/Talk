package com.yet.talk.data.repository

import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.source.post.PostRemoteSource
import com.yet.talk.api.source.social.SocialRemoteSource
import com.yet.talk.data.mapper.toPost
import com.yet.talk.data.mapper.userOf
import com.yet.talk.data.paging.FeedKeys
import com.yet.talk.data.paging.FeedPaging
import com.yet.talk.data.paging.FetchedPage
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.database.dao.ActivityDao
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.dao.ProfileDao
import com.yet.talk.database.model.ActivityRecord
import com.yet.talk.domain.model.activity.ActivityItem
import com.yet.talk.domain.repository.ActivityRepository
import com.yet.talk.domain.rule.FeedRules
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
internal class ActivityRepositoryImpl(
    private val posts: PostDao,
    private val profiles: ProfileDao,
    private val activity: ActivityDao,
    private val postsRemote: PostRemoteSource,
    private val socialRemote: SocialRemoteSource,
    private val sync: RemoteSync,
    private val ingestor: EventIngestor,
    private val session: Session,
    private val paging: FeedPaging,
) : ActivityRepository {

    override fun activity(): Flow<PagingData<ActivityItem>> =
        session.viewer.flatMapLatest { viewer ->
            if (viewer.isEmpty()) {
                flowOf(PagingData.empty())
            } else {
                paging.feed(
                    feedKey = FeedKeys.activity(viewer),
                    pages = { activity.pages(viewer) },
                    fetcher = { until -> fetch(viewer, until) },
                ).map { page -> page.filter { it.type in TYPES }.map { it.toItem(viewer) } }
            }
        }

    private suspend fun fetch(me: String, until: Long?): FetchedPage {
        val events = socialRemote.activity(me, until, FeedRules.PAGE_SIZE)
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
        sync.storeWithContext(replies)
        ingestor.ingest(events - replies.toSet())
        ingestor.ingest(socialRemote.profiles(events.map { it.event.pubkey }.toSet()))
        // No gap detection here: reactions are not counted as new items.
        return FetchedPage(events.minOfOrNull { it.event.createdAt })
    }

    private suspend fun ActivityRecord.toItem(viewer: String) = ActivityItem(
        type = TYPES.getValue(type),
        actor = userOf(actor, profiles.observe(actor).first()),
        createdAt = Instant.fromEpochSeconds(createdAt),
        eventPost = eventPostId?.let { posts.observePost(it, viewer).first()?.toPost() },
        subjectPost = subjectPostId?.let { posts.observePost(it, viewer).first()?.toPost() },
    )

    private companion object {
        const val FOLLOWERS_LIMIT = 100
        val TYPES = mapOf(
            "reply" to ActivityItem.Type.Reply,
            "quote" to ActivityItem.Type.Quote,
            "repost" to ActivityItem.Type.Repost,
            "like" to ActivityItem.Type.Like,
            "follow" to ActivityItem.Type.Follow,
        )
    }
}
