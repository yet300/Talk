package com.yet.talk.data.repository

import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.api.source.chat.ChatRemoteSource
import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.api.source.social.SocialRemoteSource
import com.yet.talk.data.mapper.userOf
import com.yet.talk.data.session.KeyVault
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.ChatDao
import com.yet.talk.database.dao.ProfileDao
import com.yet.talk.database.dao.SyncDao
import com.yet.talk.database.model.ChatMessageRecord
import com.yet.talk.database.model.ChatRecord
import com.yet.talk.database.model.PublishStates
import com.yet.talk.database.model.SyncStateRecord
import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.ChatRepository
import com.yet.talk.domain.rule.ContentRules
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.Nip44
import com.yet.talk.nostr.nip.Nip59
import com.yet.talk.nostr.nip.Rumor
import com.yet.talk.nostr.nip.values
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * NIP-17 chats. While someone is signed in, a live subscription receives their gift wraps (from their inbox relays,
 * answering NIP-42 AUTH), unwraps them and stores the messages encrypted at rest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(AppScope::class)
@Inject
internal class ChatRepositoryImpl(
    private val session: Session,
    private val chats: ChatDao,
    private val profiles: ProfileDao,
    private val syncState: SyncDao,
    private val remoteSource: ChatRemoteSource,
    private val socialRemote: SocialRemoteSource,
    private val selection: RelaySelection,
    private val ingestor: EventIngestor,
    private val vault: KeyVault,
    private val clock: Clock,
    scope: CoroutineScope,
) : ChatRepository {

    init {
        scope.launch {
            session.viewer.flatMapLatest { viewer -> if (viewer.isEmpty()) emptyFlow() else receive(viewer) }.collect { }
        }
    }

    /** Receives wraps from the viewer's inbox relays (or the defaults), from a little before the last one seen. */
    private fun receive(viewer: String): Flow<Unit> = flow {
        val inbox = runCatching { remoteSource.inboxRelays(listOf(viewer))[viewer] }.getOrNull() ?: ChatRemoteSource.DEFAULT_INBOX
        // Wrap times are random up to two days back, so the cursor is rewound by that much.
        val since = syncState.state(cursorKey(viewer))?.cursor?.let { it - Nip59.MAX_TIME_SHIFT }
        remoteSource.wraps(viewer, since, inbox).collect { wrap ->
            // A wrap that is not for us, malformed or forged is skipped.
            runCatching { receiveWrap(viewer, wrap) }
            val cursor = syncState.state(cursorKey(viewer))?.cursor ?: 0
            if (wrap.createdAt > cursor) syncState.setState(cursorKey(viewer), SyncStateRecord(false, wrap.createdAt, now()))
        }
    }

    override suspend fun prepare(): Result<Unit> = remote { ensureInbox(session.requireSigner().publicKey()) }

    /** Publishes the viewer's inbox relays (kind 10050) if they have none, so others can write to them. */
    private suspend fun ensureInbox(viewer: String): List<String> {
        remoteSource.inboxRelays(listOf(viewer))[viewer]?.let { return it }
        val signer = session.requireSigner()
        val list = signer.sign(TalkEvents.inboxRelays(ChatRemoteSource.DEFAULT_INBOX, now()))
        remoteSource.publish(list, (selection.writeRelays() + ChatRemoteSource.DEFAULT_INBOX + ChatRemoteSource.INDEXERS).distinct())
        return ChatRemoteSource.DEFAULT_INBOX
    }

    private suspend fun receiveWrap(viewer: String, wrap: Event) {
        val signer = session.signer() ?: return
        val rumor = Nip59.unwrap(wrap, signer) ?: return
        if (rumor.kind != Kinds.CHAT_MESSAGE) return
        val members = (rumor.tags.values("p") + rumor.pubkey).filter { it != viewer }.distinct()
        if (members.isEmpty()) return
        val id = ChatId.of(members.map(::UserId))
        val stored = chats.insert(id.value, id.members.map { it.value }, record(rumor, PublishStates.PUBLISHED), accepted = rumor.pubkey == viewer)
        if (stored) {
            val unknown = members.filter { profiles.observe(it).first() == null }
            if (unknown.isNotEmpty()) runCatching { ingestor.ingest(socialRemote.profiles(unknown)) }
        }
    }

    override fun chats(): Flow<List<Chat>> = session.viewer.flatMapLatest { viewer ->
        chats.observeChats(viewer).flatMapLatest { records ->
            val memberIds = records.flatMap { it.members }.distinct()
            if (memberIds.isEmpty()) {
                flowOf(records.map { it.toChat(emptyMap()) })
            } else {
                combine(memberIds.map { pubkey -> profiles.observe(pubkey).map { pubkey to userOf(pubkey, it) } }) { users ->
                    val byId = users.toMap()
                    records.map { it.toChat(byId) }
                }
            }
        }
    }

    override fun unreadCount(): Flow<Int> = chats().map { list -> list.filterNot { it.isRequest }.sumOf { it.unread } }

    override fun messages(id: ChatId): Flow<List<ChatMessage>> = session.viewer.flatMapLatest { viewer ->
        chats.observeMessages(id.value).map { records -> records.mapNotNull { it.toMessage(viewer) } }
    }

    override suspend fun canReceive(id: ChatId): Result<Boolean> = remote {
        val members = id.members.map { it.value }
        remoteSource.inboxRelays(members).keys.containsAll(members)
    }

    override suspend fun send(id: ChatId, text: String): Result<Unit> {
        val content = ContentRules.validatePost(text, isReply = true).getOrElse { return Result.failure(it) }
        return remote {
            val signer = session.requireSigner()
            val me = signer.publicKey()
            val members = id.members.map { it.value }
            ensureInbox(me)
            val rumor = Rumor.of(me, TalkEvents.chatMessage(members, content, now()))
            chats.insert(id.value, members, record(rumor, PublishStates.PENDING), accepted = true)
            deliver(rumor, members, me)
        }
    }

    override suspend fun retry(messageId: String): Result<Unit> = remote {
        val record = chats.message(messageId) ?: throw DomainError.NotFound
        val signer = session.requireSigner()
        val rumor = decrypt(record.rumorEnc)
        chats.setState(messageId, PublishStates.PENDING)
        deliver(rumor, ChatId(record.chatId).members.map { it.value }, signer.publicKey())
    }

    /** Wraps the rumor for every member and for the viewer (their other devices), each to their inbox relays. */
    private suspend fun deliver(rumor: Rumor, members: List<String>, me: String) {
        val signer = session.requireSigner()
        val result = runCatching {
            val inboxes = remoteSource.inboxRelays(members + me)
            if (!inboxes.keys.containsAll(members)) throw DomainError.CannotReceiveChats
            var delivered = false
            members.forEach { member ->
                if (remoteSource.publish(Nip59.wrap(rumor, signer, member, now()), inboxes.getValue(member))) delivered = true
            }
            remoteSource.publish(Nip59.wrap(rumor, signer, me, now()), inboxes[me] ?: ChatRemoteSource.DEFAULT_INBOX)
            if (!delivered) throw DomainError.NoRelayAccepted
        }
        chats.setState(rumor.id, if (result.isSuccess) PublishStates.PUBLISHED else PublishStates.FAILED)
        result.getOrThrow()
    }

    override suspend fun markRead(id: ChatId) = chats.markRead(id.value, now())

    override suspend fun accept(id: ChatId) = chats.accept(id.value)

    override suspend fun delete(id: ChatId) = chats.delete(id.value)

    private suspend fun record(rumor: Rumor, state: String) =
        ChatMessageRecord(rumor.id, ChatId.of((rumor.tags.values("p") + rumor.pubkey).map(::UserId)).value, rumor.pubkey, rumor.createdAt, encrypt(rumor), state)

    private suspend fun ChatRecord.toChat(users: Map<String, com.yet.talk.domain.model.user.User>) = Chat(
        id = ChatId(id),
        members = members.map { users[it] ?: userOf(it, null) },
        lastMessage = lastRumorEnc?.let { runCatching { decrypt(it).content }.getOrNull() },
        lastAt = Instant.fromEpochSeconds(lastAt),
        lastState = lastState?.toPublishState(),
        unread = unread.toInt(),
        isRequest = isRequest,
    )

    private suspend fun ChatMessageRecord.toMessage(viewer: String): ChatMessage? {
        val rumor = runCatching { decrypt(rumorEnc) }.getOrNull() ?: return null
        return ChatMessage(id, ChatId(chatId), UserId(author), author == viewer, rumor.content, Instant.fromEpochSeconds(createdAt), publishState.toPublishState())
    }

    // Chats at rest: the rumor JSON, NIP-44-encrypted under a random key kept in KSafe.
    private suspend fun encrypt(rumor: Rumor): String = Nip44.encrypt(rumor.toJson(), storageKey())

    private suspend fun decrypt(stored: String): Rumor = Rumor.fromJson(Nip44.decrypt(stored, storageKey()))

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun storageKey(): ByteArray = vault.chatKey().hexToByteArray()

    private fun String.toPublishState() = when (this) {
        PublishStates.PENDING -> PublishState.Pending
        PublishStates.FAILED -> PublishState.Failed
        else -> PublishState.Published
    }

    private fun now() = clock.now().epochSeconds

    private fun cursorKey(viewer: String) = "chats:$viewer"
}
