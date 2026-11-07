package com.yet.talk.data.repository

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.api.source.chat.ChatRemoteSource
import com.yet.talk.api.source.social.SocialRemoteSource
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.ChatDao
import com.yet.talk.database.dao.FollowDao
import com.yet.talk.database.dao.ReportDao
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.moderation.Moderation
import com.yet.talk.domain.model.moderation.ReportReason
import com.yet.talk.domain.model.moderation.ReportTarget
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.ModerationRepository
import com.yet.talk.nostr.event.Event
import dev.zacsweers.metro.AppScope
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
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
    private val chats: ChatDao,
    private val chatRemote: ChatRemoteSource,
) : ModerationRepository {

    override val moderation: Flow<Moderation> = session.viewer.flatMapLatest { viewer ->
        if (viewer.isEmpty()) {
            flowOf(Moderation())
        } else {
            combine(follows.observeFollowees(viewer), reports.observeHiddenPosts(viewer), chats.observeBlocked(viewer)) { following, hidden, blocked ->
                Moderation(
                    UserId(viewer),
                    following.mapTo(mutableSetOf(), ::UserId),
                    hidden.mapTo(mutableSetOf(), ::PostId),
                    blocked.mapTo(mutableSetOf(), ::UserId),
                )
            }
        }
    }

    override suspend fun refresh(): Result<Unit> = remote {
        val viewer = session.viewer.first().takeIf(String::isNotEmpty) ?: return@remote
        val network = follows.observeFollowees(viewer).first() + viewer
        socialRemote.reports(network, REPORTS_PER_REFRESH).forEach { store(it) }
        syncMuteList(viewer)
    }

    override suspend fun report(target: ReportTarget, reason: ReportReason): Result<Unit> = remote {
        val eventId = (target as? ReportTarget.Post)?.id?.value
        val event = publisher.sign { TalkEvents.report(target.author.value, eventId, reason.nip56, it) }
        // Hidden for the viewer at once; the report stays even if no relay takes it now.
        store(TalkEvent.parse(event) as TalkEvent.Report)
        publisher.publish(event)
    }

    override suspend fun block(id: UserId): Result<Unit> = changeBlocks(id.value, blocked = true)

    override suspend fun unblock(id: UserId): Result<Unit> = changeBlocks(id.value, blocked = false)

    /**
     * Rewrites the mute list from its newest copy on relays, so entries added elsewhere survive. Blocks are private
     * entries (NIP-44 to yourself); a list whose private part Talk cannot read (NIP-04) is left alone.
     */
    private suspend fun changeBlocks(pubkey: String, blocked: Boolean): Result<Unit> = remote {
        val signer = session.requireSigner()
        val me = signer.publicKey()
        val current = syncMuteList(me)
        val entry = listOf("p", pubkey)
        val isEntry = { tag: List<String> -> tag.firstOrNull() == "p" && tag.getOrNull(1) == pubkey }
        val private = current?.let { privateTags(it, me) ?: throw DomainError.UnreadableMuteList }.orEmpty().filterNot(isEntry)
        val newPrivate = if (blocked) private + listOf(entry) else private
        // Unblocking also drops a public entry another app may have written.
        val public = current?.tags.orEmpty().let { tags -> if (blocked) tags else tags.filterNot(isEntry) }
        val content = signer.nip44Encrypt(json.encodeToString(tagsSerializer, newPrivate), me)
        // Strictly newer than the list it replaces, so relays keep this one.
        val event = publisher.sign { now -> TalkEvents.muteList(public, content, maxOf(now, (current?.createdAt ?: 0) + 1)) }
        storeMuteList(event, me)
        publisher.publish(event)
    }

    /** Fetches and stores the viewer's newest mute list; returns the newest one known. */
    private suspend fun syncMuteList(me: String): Event? {
        runCatching { chatRemote.muteList(me) }.getOrNull()?.let { storeMuteList(it, me) }
        return chats.muteList(me)?.let { Event.fromJson(it) }
    }

    private suspend fun storeMuteList(event: Event, me: String) {
        if (event.pubkey != me) return
        chats.storeMuteList(me, event.createdAt, event.toJson())
        val newest = chats.muteList(me)?.let { Event.fromJson(it) } ?: return
        val private = privateTags(newest, me).orEmpty()
        chats.setBlocked(me, (newest.tags + private).filter { it.firstOrNull() == "p" }.mapNotNull { it.getOrNull(1) }.toSet())
    }

    /** The decrypted private entries, empty when there are none, null when they are unreadable. */
    private suspend fun privateTags(list: Event, me: String): List<List<String>>? {
        if (list.content.isBlank()) return emptyList()
        val signer = session.signer() ?: return null
        return runCatching { json.decodeFromString(tagsSerializer, signer.nip44Decrypt(list.content, me)) }.getOrNull()
    }

    private suspend fun store(report: TalkEvent.Report) = with(report) {
        reports.insert(event.id, event.pubkey, pubkey, eventId, type, event.createdAt)
    }

    private companion object {
        const val REPORTS_PER_REFRESH = 500
        val tagsSerializer = ListSerializer(ListSerializer(String.serializer()))
        val json = Json
    }
}
