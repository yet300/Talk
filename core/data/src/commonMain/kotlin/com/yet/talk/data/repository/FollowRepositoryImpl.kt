package com.yet.talk.data.repository

import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.data.sync.publishReplaceable
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.FollowDao
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.FollowRepository
import com.yet.talk.nostr.nip.contactList
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
internal class FollowRepositoryImpl(
    private val follows: FollowDao,
    private val sync: RemoteSync,
    private val ingestor: EventIngestor,
    private val session: Session,
    private val publisher: Publisher,
) : FollowRepository {

    override fun observeIsFollowing(id: UserId): Flow<Boolean> =
        session.viewer.flatMapLatest { viewer ->
            if (viewer.isEmpty()) flowOf(false) else follows.observeIsFollowing(viewer, id.value)
        }

    override suspend fun setFollowing(id: UserId, following: Boolean): Result<Unit> = remote {
        val me = publisher.me()
        if (id.value == me) return@remote
        sync.ensureListsSynced(me)
        val current = follows.observeFollowees(me).first()
        val updated = if (following) current + id.value else current - id.value
        if (updated.toSet() == current.toSet()) return@remote
        publishReplaceable(follows.version(me), ingestor, publisher) { contactList(updated, it) }
    }
}
