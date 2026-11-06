package com.yet.talk.data.repository

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.source.social.SocialRemoteSource
import com.yet.talk.data.mapper.userOf
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.ProfileDao
import com.yet.talk.database.dao.FollowDao
import com.yet.talk.domain.model.user.ProfileEdit
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.model.user.UserStats
import com.yet.talk.domain.repository.ProfileRepository
import com.yet.talk.api.source.nip05.Nip05Verifier
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.mergeMetadataContent
import com.yet.talk.nostr.nip.ExternalIdentity
import com.yet.talk.nostr.nip.replaceIdentity
import com.yet.talk.data.github.GitHubProofs
import kotlinx.coroutines.flow.first
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@SingleIn(AppScope::class)
@Inject
internal class ProfileRepositoryImpl(
    private val profiles: ProfileDao,
    private val follows: FollowDao,
    private val socialRemote: SocialRemoteSource,
    private val sync: RemoteSync,
    private val ingestor: EventIngestor,
    private val publisher: Publisher,
    private val nip05: Nip05Verifier,
    private val gitHubProofs: GitHubProofs,
) : ProfileRepository {
    /** NIP-45 counts are not cached in the database: they are only meaningful fresh. */
    private val counts = MutableStateFlow<Map<String, Pair<Long?, Long?>>>(emptyMap())

    override fun observeUser(id: UserId): Flow<User> = profiles.observe(id.value).map { userOf(id.value, it) }

    override fun observeStats(id: UserId): Flow<UserStats> =
        combine(follows.observeFollowingCount(id.value), counts) { following, counts ->
            val (followers, posts) = counts[id.value] ?: (null to null)
            UserStats(followers = followers, following = following.toInt(), posts = posts)
        }

    override suspend fun refresh(id: UserId): Result<Unit> = remote {
        ingestor.ingest(socialRemote.profiles(listOf(id.value)))
        checkGitHub(id.value)
        sync.syncLists(id.value)
        val followers = socialRemote.countFollowers(id.value)
        val posts = socialRemote.countPosts(id.value)
        counts.update { it + (id.value to (followers to posts)) }
    }

    /**
     * Merges [edit] into the latest kind 0 on relays, so fields Talk does not edit (`lud16`, other apps' keys)
     * survive. When relays return no kind 0 but one is cached, it exists and could not be read: rather than
     * overwrite it with only Talk's fields, this fails as a network error.
     */
    override suspend fun updateMyProfile(edit: ProfileEdit): Result<Unit> = remote {
        val me = publisher.me()
        val latest = socialRemote.profiles(listOf(me)).maxByOrNull { it.event.createdAt }
        if (latest == null && profiles.observe(me).first() != null) throw DomainError.Network()
        val content = mergeMetadataContent(
            previous = latest?.event?.content,
            changes = with(edit) {
                // A null picture or banner keeps the current one; a blank text field removes its key.
                mapOf("name" to name, "display_name" to displayName, "about" to about, "website" to website, "nip05" to nip05) +
                    listOfNotNull(picture?.let { "picture" to it }, banner?.let { "banner" to it })
            },
        )
        val previousTags = latest?.event?.tags.orEmpty()
        val tags = edit.github?.let { replaceIdentity(previousTags, GITHUB, ExternalIdentity(GITHUB, it.login, it.gistId)) } ?: previousTags
        val previousAt = latest?.event?.createdAt ?: 0
        val event = publisher.sign { now -> EventTemplate(Kinds.METADATA, content, tags, createdAt = maxOf(now, previousAt + 1)) }
        ingestor.ingest(listOfNotNull(TalkEvent.parse(event)))
        // The editor verified this link a moment ago.
        edit.github?.let { profiles.setGitHubCheck(me, it.login, it.gistId, verified = true, checkedAt = publisher.now()) }
        publisher.publish(event)
    }

    /**
     * Checks [pubkey]'s GitHub claim when it was never checked or the last check is a day old. GitHub's limit
     * (60 requests an hour) or a network failure leaves it unchecked, so it is not shown.
     */
    private suspend fun checkGitHub(pubkey: String) {
        val record = profiles.observe(pubkey).first() ?: return
        val login = record.githubLogin ?: return
        val proof = record.githubProof ?: return
        val checkedAt = record.githubCheckedAt
        if (checkedAt != null && publisher.now() - checkedAt < GITHUB_RECHECK_SECONDS) return
        val proves = runCatching { gitHubProofs.check(pubkey, login, proof) != null }.getOrElse { return }
        profiles.setGitHubCheck(pubkey, login, proof, proves, publisher.now())
    }

    override suspend fun verifyNip05(id: UserId, identifier: String): Result<Boolean> = remote {
        nip05.verify(identifier, id.value)
    }

    private companion object {
        const val GITHUB = "github"
        const val GITHUB_RECHECK_SECONDS = 24 * 60 * 60L
    }
}
