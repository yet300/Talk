package com.yet.talk.data.repository

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.source.social.SocialRemoteSource
import com.yet.talk.data.mapper.userOf
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.ProfileDao
import com.yet.talk.database.dao.SocialDao
import com.yet.talk.domain.model.user.ProfileEdit
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.model.user.UserStats
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.nostr.nip.Metadata
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class ProfileRepositoryImpl(
    private val profiles: ProfileDao,
    private val social: SocialDao,
    private val socialRemote: SocialRemoteSource,
    private val sync: RemoteSync,
    private val ingestor: EventIngestor,
    private val publisher: Publisher,
) : ProfileRepository {
    /** NIP-45 counts are not cached in the database: they are only meaningful fresh. */
    private val counts = MutableStateFlow<Map<String, Pair<Long?, Long?>>>(emptyMap())

    override fun observeUser(id: UserId): Flow<User> = profiles.observe(id.value).map { userOf(id.value, it) }

    override fun observeStats(id: UserId): Flow<UserStats> =
        combine(social.observeFollowingCount(id.value), counts) { following, counts ->
            val (followers, posts) = counts[id.value] ?: (null to null)
            UserStats(followers = followers, following = following.toInt(), posts = posts)
        }

    override suspend fun refresh(id: UserId): Result<Unit> = remote {
        ingestor.ingest(socialRemote.profiles(listOf(id.value)))
        sync.syncLists(id.value)
        val followers = socialRemote.countFollowers(id.value)
        val posts = socialRemote.countPosts(id.value)
        counts.update { it + (id.value to (followers to posts)) }
    }

    override suspend fun updateMyProfile(edit: ProfileEdit): Result<Unit> = remote {
        val metadata = with(edit) { Metadata(name = name, displayName = displayName, about = about, picture = picture, website = website, nip05 = nip05) }
        val event = publisher.sign { metadata.toTemplate(it) }
        ingestor.ingest(listOfNotNull(TalkEvent.parse(event)))
        publisher.publish(event)
    }
}
