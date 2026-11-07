package com.yet.talk.data.repository

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.api.source.social.SocialRemoteSource
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.FollowDao
import com.yet.talk.database.dao.ReportDao
import com.yet.talk.domain.model.moderation.Moderation
import com.yet.talk.domain.model.moderation.ReportReason
import com.yet.talk.domain.model.moderation.ReportTarget
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.ModerationRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(AppScope::class)
@Inject
internal class ModerationRepositoryImpl(
    private val session: Session,
    private val follows: FollowDao,
    private val reports: ReportDao,
    private val socialRemote: SocialRemoteSource,
    private val publisher: Publisher,
) : ModerationRepository {

    override val moderation: Flow<Moderation> = session.viewer.flatMapLatest { viewer ->
        if (viewer.isEmpty()) {
            flowOf(Moderation())
        } else {
            combine(follows.observeFollowees(viewer), reports.observeHiddenPosts(viewer)) { following, hidden ->
                Moderation(UserId(viewer), following.mapTo(mutableSetOf(), ::UserId), hidden.mapTo(mutableSetOf(), ::PostId))
            }
        }
    }

    override suspend fun refresh(): Result<Unit> = remote {
        val viewer = session.viewer.first().takeIf(String::isNotEmpty) ?: return@remote
        val network = follows.observeFollowees(viewer).first() + viewer
        socialRemote.reports(network, REPORTS_PER_REFRESH).forEach { store(it) }
    }

    override suspend fun report(target: ReportTarget, reason: ReportReason): Result<Unit> = remote {
        val eventId = (target as? ReportTarget.Post)?.id?.value
        val event = publisher.sign { TalkEvents.report(target.author.value, eventId, reason.nip56, it) }
        // Hidden for the viewer at once; the report stays even if no relay takes it now.
        store(TalkEvent.parse(event) as TalkEvent.Report)
        publisher.publish(event)
    }

    private suspend fun store(report: TalkEvent.Report) = with(report) {
        reports.insert(event.id, event.pubkey, pubkey, eventId, type, event.createdAt)
    }

    private companion object {
        const val REPORTS_PER_REFRESH = 500
    }
}
