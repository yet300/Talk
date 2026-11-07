package com.yet.talk.data.repository

import com.yet.talk.api.schema.ChatFileMeta
import com.yet.talk.api.schema.ChatRumor
import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.api.source.chat.ChatRemoteSource
import com.yet.talk.api.source.media.MediaRemoteSource
import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.api.source.social.SocialRemoteSource
import com.yet.talk.data.mapper.userOf
import com.yet.talk.data.session.ChatKeyring
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.Outbox
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.ChatDao
import com.yet.talk.database.dao.ProfileDao
import com.yet.talk.database.dao.SyncDao
import com.yet.talk.database.model.ChatMessageRecord
import com.yet.talk.database.model.ChatReactionRecord
import com.yet.talk.database.model.ChatRecord
import com.yet.talk.database.model.MessageRemoval
import com.yet.talk.database.model.PublishStates
import com.yet.talk.database.model.SyncStateRecord
import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.ChatImage
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.domain.model.chat.Disappearing
import com.yet.talk.domain.model.chat.SafetyCode
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.ChatRepository
import com.yet.talk.domain.rule.ContentRules
import com.yet.talk.nostr.crypto.AesGcm
import com.yet.talk.nostr.crypto.Sha256
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.nip.Nip44
import com.yet.talk.nostr.nip.Nip59
import com.yet.talk.nostr.nip.Rumor
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * NIP-17 chats. While someone is signed in, a live subscription receives their gift wraps (from their inbox relays,
 * answering NIP-42 AUTH), unwraps them and stores messages, reactions and deletions, encrypted at rest.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
@SingleIn(AppScope::class)
@Inject
internal class ChatRepositoryImpl(
    private val session: Session,
    private val chats: ChatDao,
    private val profiles: ProfileDao,
    private val syncState: SyncDao,
    private val remoteSource: ChatRemoteSource,
    private val media: MediaRemoteSource,
    private val socialRemote: SocialRemoteSource,
    private val selection: RelaySelection,
    private val ingestor: EventIngestor,
    private val keyring: ChatKeyring,
    private val outbox: Outbox,
    private val clock: Clock,
    scope: CoroutineScope,
) : ChatRepository {

    // Decrypted rumors by message id (decrypting every message on each change would be slow), and recent images.
    private val cacheLock = Mutex()
    private val rumors = mutableMapOf<String, Rumor>()
    private val images = LinkedHashMap<String, ByteArray>()

    init {
        scope.launch {
            session.viewer.flatMapLatest { viewer ->
                cacheLock.withLock {
                    rumors.clear()
                    images.clear()
                }
                if (viewer.isEmpty()) emptyFlow() else receive(viewer)
            }.collect { }
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
        val parsed = Nip59.unwrap(wrap, signer)?.let(ChatRumor::parse) ?: return
        val members = parsed.participants.filter { it != viewer }
        if (members.isEmpty() || members.size > ChatRepository.MAX_GROUP_MEMBERS) return
        val id = ChatId.of(members.map(::UserId))
        val rumor = parsed.rumor
        when (parsed) {
            is ChatRumor.Message -> {
                // Already gone: never shown.
                if (parsed.expiresAt?.let { it <= now() } == true) return
                val stored = chats.insert(id.value, id.members.map { it.value }, record(parsed, id, PublishStates.PUBLISHED), accepted = rumor.pubkey == viewer)
                if (!stored) return
                parsed.subject?.let { chats.setName(id.value, encrypt(it)) }
                val unknown = members.filter { profiles.observe(it).first() == null }
                if (unknown.isNotEmpty()) runCatching { ingestor.ingest(socialRemote.profiles(unknown)) }
            }
            is ChatRumor.Reaction -> chats.react(parsed.target, rumor.pubkey, parsed.emoji, rumor.createdAt)
            // Delete for everyone counts only for the author's own messages in this chat.
            is ChatRumor.Deletion -> parsed.targets.forEach { target ->
                val message = chats.message(target)
                if (message != null && message.author == rumor.pubkey && message.chatId == id.value) remove(target, MessageRemoval.BY_AUTHOR)
            }
        }
    }

    override fun chats(): Flow<List<Chat>> = session.viewer.flatMapLatest { viewer ->
        combine(chats.observeChats(viewer), chats.observeVerified(viewer)) { records, verified -> records to verified }.flatMapLatest { (records, verified) ->
            val memberIds = records.flatMap { it.members }.distinct()
            if (memberIds.isEmpty()) {
                flowOf(records.map { it.toChat(emptyMap(), viewer, verified) })
            } else {
                combine(memberIds.map { pubkey -> profiles.observe(pubkey).map { pubkey to userOf(pubkey, it) } }) { users ->
                    val byId = users.toMap()
                    records.map { it.toChat(byId, viewer, verified) }
                }
            }
        }
    }

    override fun unreadCount(): Flow<Int> = chats().map { list -> list.filterNot { it.isRequest || it.isMuted }.sumOf { it.unread } }

    override fun chat(id: ChatId): Flow<Chat?> = chats().map { list -> list.firstOrNull { it.id == id } }

    override fun messages(id: ChatId): Flow<List<ChatMessage>> = session.viewer.flatMapLatest { viewer ->
        combine(chats.observeMessages(id.value), chats.observeReactions(id.value), expiryTicks()) { records, reactions, _ ->
            toMessages(records, reactions, viewer)
        }.onStart { chats.expire(now()) }
    }

    /** Turns messages whose time is up into placeholders every half minute while a chat is open. */
    private fun expiryTicks(): Flow<Unit> = flow {
        emit(Unit)
        while (true) {
            delay(30.seconds)
            chats.expire(now())
            emit(Unit)
        }
    }

    override suspend fun canReceive(id: ChatId): Result<Boolean> = remote {
        val members = id.members.map { it.value }
        remoteSource.inboxRelays(members).keys.containsAll(members)
    }

    override suspend fun createGroup(id: ChatId, name: String?): Result<Unit> {
        if (id.members.size > ChatRepository.MAX_GROUP_MEMBERS) return Result.failure(DomainError.TooManyMembers(ChatRepository.MAX_GROUP_MEMBERS))
        return runCatching {
            chats.ensureChat(id.value, id.members.map { it.value }, now())
            name?.trim()?.takeIf(String::isNotEmpty)?.let { chats.setName(id.value, encrypt(it)) }
        }
    }

    override suspend fun send(id: ChatId, text: String, replyTo: String?): Result<Unit> {
        val content = ContentRules.validatePost(text, isReply = true).getOrElse { return Result.failure(it) }
        return remote {
            val (subject, expiresAt) = outgoing(id)
            sendRumor(id) { me, members -> TalkEvents.chatMessage(members, content, now(), replyTo, subject, expiresAt) }
        }
    }

    override suspend fun sendImage(id: ChatId, file: ImageFile): Result<Unit> {
        ContentRules.validateImage(file).getOrElse { return Result.failure(it) }
        return remote {
            val signer = session.requireSigner()
            // The file is encrypted before it leaves the device; its key and nonce travel only inside the wrap.
            val sealed = AesGcm.encrypt(file.bytes)
            val blob = media.upload(sealed.ciphertext, ENCRYPTED_TYPE, signer, now())
            val meta = ChatFileMeta(
                mimeType = file.mimeType,
                key = sealed.key.toHexString(),
                nonce = sealed.nonce.toHexString(),
                hash = Sha256.hex(sealed.ciphertext),
                originalHash = Sha256.hex(file.bytes),
                size = file.bytes.size.toLong(),
            )
            val (subject, expiresAt) = outgoing(id)
            sendRumor(id) { _, members -> TalkEvents.chatFile(members, blob.url, meta, now(), subject, expiresAt) }
        }
    }

    /** A group's name goes with every message (NIP-17 `subject`), and the chat's disappearing time. */
    private suspend fun outgoing(id: ChatId): Pair<String?, Long?> {
        val (nameEnc, expireSeconds) = chats.settings(id.value) ?: (null to null)
        val subject = if (id.isGroup) nameEnc?.let { runCatching { decryptText(it) }.getOrNull() } else null
        return subject to expireSeconds?.let { now() + it }
    }

    private suspend fun sendRumor(id: ChatId, build: (me: String, members: List<String>) -> com.yet.talk.nostr.event.EventTemplate) {
        val signer = session.requireSigner()
        val me = signer.publicKey()
        val members = id.members.map { it.value }
        ensureInbox(me)
        val rumor = Rumor.of(me, build(me, members))
        val parsed = ChatRumor.parse(rumor) as ChatRumor.Message
        chats.insert(id.value, members, record(parsed, id, PublishStates.PENDING), accepted = true)
        deliver(rumor, members, me)
    }

    override suspend fun image(messageId: String): Result<ByteArray> = remote {
        cacheLock.withLock { images[messageId] }?.let { return@remote it }
        val record = chats.message(messageId) ?: throw DomainError.NotFound
        val file = ChatRumor.parse(decrypt(record)) as? ChatRumor.File ?: throw DomainError.NotFound
        val ciphertext = media.download(file.url)
        // The blob must be the one the sender named, or it is not decrypted at all.
        if (!Sha256.hex(ciphertext).equals(file.meta.hash, ignoreCase = true)) throw DomainError.NotFound
        val bytes = AesGcm.decrypt(ciphertext, file.meta.key.hexToByteArray(), file.meta.nonce.hexToByteArray())
        cacheLock.withLock {
            images[messageId] = bytes
            while (images.size > MAX_CACHED_IMAGES) images.remove(images.keys.first())
        }
        bytes
    }

    override suspend fun retry(messageId: String): Result<Unit> = remote {
        val record = chats.message(messageId) ?: throw DomainError.NotFound
        val signer = session.requireSigner()
        val rumor = decrypt(record)
        chats.setState(messageId, PublishStates.PENDING)
        deliver(rumor, ChatId(record.chatId).members.map { it.value }, signer.publicKey())
    }

    override suspend fun react(messageId: String, emoji: String): Result<Unit> = remote {
        val message = chats.message(messageId) ?: throw DomainError.NotFound
        val signer = session.requireSigner()
        val me = signer.publicKey()
        val members = ChatId(message.chatId).members.map { it.value }
        val rumor = Rumor.of(me, TalkEvents.chatReaction(members, messageId, message.author, emoji, message.kind.toInt(), now()))
        chats.react(messageId, me, emoji, rumor.createdAt)
        send(rumor, members, me)
    }

    override suspend fun deleteForEveryone(messageId: String): Result<Unit> = remote {
        val message = chats.message(messageId) ?: throw DomainError.NotFound
        val signer = session.requireSigner()
        val me = signer.publicKey()
        if (message.author != me) throw DomainError.NotFound
        // Never sent: nobody else has it.
        if (message.publishState == PublishStates.PUBLISHED) {
            val members = ChatId(message.chatId).members.map { it.value }
            // Published first: the deletion cannot be taken back.
            send(Rumor.of(me, TalkEvents.chatDeletion(members, listOf(messageId), now())), members, me)
        }
        remove(messageId, MessageRemoval.BY_AUTHOR)
    }

    override suspend fun deleteForMe(messageId: String) = remove(messageId, MessageRemoval.FOR_ME)

    private suspend fun remove(messageId: String, removal: Long) {
        chats.remove(messageId, removal)
        cacheLock.withLock {
            rumors.remove(messageId)
            images.remove(messageId)
        }
    }

    override suspend fun setDisappearing(id: ChatId, disappearing: Disappearing) {
        chats.ensureChat(id.value, id.members.map { it.value }, now())
        chats.setExpireSeconds(id.value, disappearing.duration?.inWholeSeconds)
    }

    override suspend fun setMuted(id: ChatId, muted: Boolean) = chats.setMuted(id.value, muted)

    override suspend fun leave(id: ChatId) = chats.setHidden(id.value, true)

    /** Wraps the message for every member and for the viewer, then marks it sent or Not sent. */
    private suspend fun deliver(rumor: Rumor, members: List<String>, me: String) {
        val result = runCatching { send(rumor, members, me) }
        chats.setState(rumor.id, if (result.isSuccess) PublishStates.PUBLISHED else PublishStates.FAILED)
        result.getOrThrow()
    }

    /**
     * One wrap per member to their inbox relays, plus one to the viewer's (their other devices). A disappearing
     * message's wraps carry the same NIP-40 expiration, so relays may drop them.
     */
    private suspend fun send(rumor: Rumor, members: List<String>, me: String) {
        val signer = session.requireSigner()
        val inboxes = remoteSource.inboxRelays(members + me)
        if (!inboxes.keys.containsAll(members)) throw DomainError.CannotReceiveChats
        val expiration = ChatRumor.parse(rumor)?.expiresAt?.let { listOf(listOf("expiration", it.toString())) }.orEmpty()
        var delivered = false
        // Through the outbox: an inbox relay that refuses for now gets the same wrap again later.
        members.forEach { member ->
            if (outbox.publish(Nip59.wrap(rumor, signer, member, now(), expiration), inboxes.getValue(member)).isAccepted) delivered = true
        }
        outbox.publish(Nip59.wrap(rumor, signer, me, now(), expiration), inboxes[me] ?: ChatRemoteSource.DEFAULT_INBOX)
        if (!delivered) throw DomainError.NoRelayAccepted
    }

    override suspend fun safetyCode(person: UserId): Result<SafetyCode> = runCatching {
        SafetyCodes.of(session.requireSigner().publicKey(), person.value)
    }

    override fun verified(): Flow<Map<UserId, Instant>> = session.viewer.flatMapLatest { viewer ->
        chats.observeVerified(viewer).map { all -> all.entries.associate { (pubkey, at) -> UserId(pubkey) to Instant.fromEpochSeconds(at) } }
    }

    override suspend fun setVerified(person: UserId, verified: Boolean) {
        val me = session.requireSigner().publicKey()
        chats.setVerified(me, person.value, if (verified) now() else null)
    }

    override suspend fun verifyScanned(person: UserId, payload: String): Result<Boolean> = runCatching {
        val matches = SafetyCodes.of(session.requireSigner().publicKey(), person.value).qrPayload == payload.trim()
        if (matches) setVerified(person, true)
        matches
    }

    override suspend fun eraseAll() {
        val viewer = session.viewer.first()
        chats.eraseChats()
        keyring.erase()
        cacheLock.withLock {
            rumors.clear()
            images.clear()
        }
        // Relays may still hold the wraps: they come back on the next start while they are kept there.
        if (viewer.isNotEmpty()) syncState.reset(cursorKey(viewer))
    }

    override suspend fun markRead(id: ChatId) = chats.markRead(id.value, now())

    override suspend fun accept(id: ChatId) = chats.accept(id.value)

    override suspend fun delete(id: ChatId) = chats.delete(id.value)

    private suspend fun record(message: ChatRumor.Message, id: ChatId, state: String) = with(message.rumor) {
        ChatMessageRecord(this.id, id.value, pubkey, createdAt, encrypt(this), state, kind.toLong(), message.replyTo, message.expiresAt)
    }

    private suspend fun ChatRecord.toChat(users: Map<String, User>, viewer: String, verified: Map<String, Long>): Chat {
        val last = lastRumorEnc?.takeIf(String::isNotEmpty)?.let { runCatching { decryptStored(it) }.getOrNull() }
        return Chat(
            id = ChatId(id),
            members = members.map { users[it] ?: userOf(it, null) },
            name = nameEnc?.let { runCatching { decryptText(it) }.getOrNull() },
            lastMessage = last?.takeIf { it.kind != com.yet.talk.nostr.nip.Kinds.CHAT_FILE }?.content,
            lastIsImage = last?.kind == com.yet.talk.nostr.nip.Kinds.CHAT_FILE,
            lastIsMine = last?.pubkey == viewer,
            lastAt = Instant.fromEpochSeconds(lastAt),
            lastState = lastState?.toPublishState(),
            unread = unread.toInt(),
            isRequest = isRequest,
            isMuted = muted,
            disappearing = expireSeconds?.seconds,
            isVerified = members.size == 1 && members.single() in verified,
        )
    }

    private suspend fun toMessages(records: List<ChatMessageRecord>, reactions: List<ChatReactionRecord>, viewer: String): List<ChatMessage> {
        val parsed = records.associate { record ->
            record.id to record.rumorEnc.takeIf(String::isNotEmpty)?.let { runCatching { ChatRumor.parse(decrypt(record)) }.getOrNull() }
        }
        val byMessage = reactions.groupBy { it.messageId }
        return records.mapNotNull { record ->
            val base = ChatMessage(
                id = record.id,
                chatId = ChatId(record.chatId),
                author = UserId(record.author),
                isMine = record.author == viewer,
                content = "",
                createdAt = Instant.fromEpochSeconds(record.createdAt),
                publishState = record.publishState.toPublishState(),
                expiresAt = record.expiresAt?.let(Instant::fromEpochSeconds),
            )
            if (record.removed == MessageRemoval.EXPIRED) return@mapNotNull base.copy(isExpired = true)
            val rumor = parsed[record.id] as? ChatRumor.Message ?: return@mapNotNull null
            base.copy(
                content = (rumor as? ChatRumor.Text)?.rumor?.content.orEmpty(),
                image = (rumor as? ChatRumor.File)?.let { ChatImage(record.id, it.meta.mimeType, it.meta.size) },
                replyTo = record.replyTo?.let { target -> quote(target, records, parsed) },
                reactions = byMessage[record.id].orEmpty()
                    .groupBy { it.emoji }
                    .map { (emoji, list) -> ChatMessage.Reaction(emoji, list.size, list.any { it.author == viewer }) },
            )
        }
    }

    private fun quote(target: String, records: List<ChatMessageRecord>, parsed: Map<String, ChatRumor?>): ChatMessage.Quote {
        val record = records.firstOrNull { it.id == target }
        val rumor = parsed[target]
        return ChatMessage.Quote(
            id = target,
            author = UserId(record?.author.orEmpty()),
            text = (rumor as? ChatRumor.Text)?.rumor?.content,
            isImage = rumor is ChatRumor.File,
        )
    }

    // Chats at rest: the rumor JSON, NIP-44-encrypted under a random key kept in KSafe.
    private suspend fun encrypt(rumor: Rumor): String = Nip44.encrypt(rumor.toJson(), storageKey())

    private suspend fun encrypt(text: String): String = Nip44.encrypt(text, storageKey())

    private suspend fun decryptText(stored: String): String = Nip44.decrypt(stored, storageKey())

    private suspend fun decryptStored(stored: String): Rumor = Rumor.fromJson(Nip44.decrypt(stored, storageKey()))

    private suspend fun decrypt(record: ChatMessageRecord): Rumor {
        cacheLock.withLock { rumors[record.id] }?.let { return it }
        val rumor = decryptStored(record.rumorEnc)
        cacheLock.withLock { rumors[record.id] = rumor }
        return rumor
    }

    private suspend fun storageKey(): ByteArray = keyring.key()

    private fun String.toPublishState() = when (this) {
        PublishStates.PENDING -> PublishState.Pending
        PublishStates.FAILED -> PublishState.Failed
        else -> PublishState.Published
    }

    private fun now() = clock.now().epochSeconds

    private fun cursorKey(viewer: String) = "chats:$viewer"

    private companion object {
        /** What Blossom is told an encrypted file is: it cannot tell anyway. */
        const val ENCRYPTED_TYPE = "application/octet-stream"
        const val MAX_CACHED_IMAGES = 30
    }
}
