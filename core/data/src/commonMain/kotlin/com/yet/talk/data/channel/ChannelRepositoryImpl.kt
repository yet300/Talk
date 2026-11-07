package com.yet.talk.data.channel

import com.yet.talk.api.schema.ChannelEvents
import com.yet.talk.api.source.channel.ChannelRemoteSource
import com.yet.talk.data.sync.remote
import com.yet.talk.domain.model.channel.ChannelLevel
import com.yet.talk.domain.model.channel.ChannelMessage
import com.yet.talk.domain.model.channel.ChannelRoom
import com.yet.talk.domain.model.channel.GeoPoint
import com.yet.talk.domain.model.channel.Geohash
import com.yet.talk.domain.model.channel.LocationChannel
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.repository.ChannelRepository
import com.yet.talk.domain.repository.PreferencesRepository
import com.yet.talk.domain.rule.ContentRules
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.Nip13
import com.yet.talk.nostr.signer.LocalKeySigner
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Location channels as bitchat speaks them: kind 20000 messages and kind 20001 presence with a `g` tag, signed with
 * a key per area ([ChannelKeys]), on the five relays nearest the area ([GeoRelays]). Nothing is stored: a room
 * shows the last hour that relays still hold (ephemeral events usually are not) and what arrives while it is open.
 */
@SingleIn(AppScope::class)
@Inject
internal class ChannelRepositoryImpl(
    private val remoteSource: ChannelRemoteSource,
    private val keys: ChannelKeys,
    private val preferences: PreferencesRepository,
    private val clock: Clock,
) : ChannelRepository {

    /** What we sent, shown at once (relays may not echo it back to the sender). */
    private val sent = MutableSharedFlow<Event>(extraBufferCapacity = 16)

    override fun channelsAround(point: GeoPoint): List<LocationChannel> =
        ChannelLevel.entries.map { LocationChannel(Geohash.encode(point.latitude, point.longitude, it.precision), it, isTeleport = false) }

    override fun channel(geohash: String, here: GeoPoint?): LocationChannel? {
        val code = Geohash.normalize(geohash)
        if (!Geohash.isValid(code)) return null
        val level = ChannelLevel.of(code) ?: return null
        val local = here?.let { Geohash.encode(it.latitude, it.longitude, code.length) }
        return LocationChannel(code, level, isTeleport = local != code)
    }

    override fun peopleCounts(channels: List<LocationChannel>): Flow<Map<String, Int?>> {
        val counted = channels.filter { it.level.sendsPresence }.map { it.geohash }
        val unknown = channels.filterNot { it.level.sendsPresence }.associate { it.geohash to null as Int? }
        if (counted.isEmpty()) return flowOf(unknown)
        // One subscription for all: the finest counted area's relays are the nearest for the coarser ones too.
        val relays = GeoRelays.nearest(counted.maxBy { it.length })
        return channelFlow {
            val seen = Seen()
            send(unknown + counted.associateWith { 0 })
            launch {
                remoteSource.events(counted, now() - WINDOW_SECONDS, relays).collect { event ->
                    ChannelEvents.geohash(event)?.takeIf { it in counted }?.let { seen.record(it, event.pubkey, event.createdAt, now()) }
                    send(unknown + seen.counts(counted, now()))
                }
            }
            // Counts fall as people go quiet.
            while (true) {
                delay(COUNT_REFRESH)
                send(unknown + seen.counts(counted, now()))
            }
        }
    }

    override fun room(channel: LocationChannel): Flow<ChannelRoom> = channelFlow {
        val area = channel.geohash
        val me = keys.forArea(area).publicKey
        val nickname = currentNickname()
        val lock = Mutex()
        val messages = linkedMapOf<String, ChannelMessage>()
        val seen = Seen()
        suspend fun emitRoom() = send(
            ChannelRoom(
                messages = messages.values.sortedWith(compareBy({ it.createdAt }, { it.id })),
                people = if (channel.level.sendsPresence) seen.counts(listOf(area), now())[area] else null,
            ),
        )
        suspend fun accept(event: Event) = lock.withLock {
            if (ChannelEvents.geohash(event) != area) return@withLock
            seen.record(area, event.pubkey, event.createdAt, now())
            if (event.kind == Kinds.GEOHASH_MESSAGE && event.content.isNotBlank() && event.id !in messages) {
                val isMine = event.pubkey == me
                val nick = ChannelEvents.nickname(event) ?: if (isMine) nickname else ChannelRepository.DEFAULT_NICKNAME
                messages[event.id] = ChannelMessage(
                    id = event.id,
                    author = event.pubkey,
                    displayName = "$nick#${event.pubkey.takeLast(4)}",
                    content = event.content,
                    createdAt = Instant.fromEpochSeconds(minOf(event.createdAt, now())),
                    isMine = isMine,
                    isTeleported = ChannelEvents.isTeleported(event),
                )
                while (messages.size > MAX_MESSAGES) messages.remove(messages.keys.first())
            }
            emitRoom()
        }
        emitRoom()
        launch { sent.collect { accept(it) } }
        launch { remoteSource.events(listOf(area), now() - HISTORY_SECONDS, GeoRelays.nearest(area)).collect { accept(it) } }
        while (true) {
            delay(COUNT_REFRESH)
            lock.withLock { emitRoom() }
        }
    }

    override suspend fun send(channel: LocationChannel, text: String): Result<Unit> {
        val content = ContentRules.validatePost(text, isReply = true).getOrElse { return Result.failure(it) }
        return remote {
            val key = keys.forArea(channel.geohash)
            val template = ChannelEvents.message(channel.geohash, content, currentNickname(), channel.isTeleport, now())
            // Proof of work (NIP-13), off the main thread: bitchat clients that filter by work accept it.
            val mined = withContext(Dispatchers.Default) { Nip13.mine(template, key.publicKey, POW_BITS) }
            val event = LocalKeySigner(key).sign(mined)
            sent.emit(event)
            if (!remoteSource.publish(event, GeoRelays.nearest(channel.geohash))) throw DomainError.NoRelayAccepted
        }
    }

    override fun announce(channels: List<LocationChannel>): Flow<Unit> = flow {
        val targets = channels.filter { !it.isTeleport && it.level.sendsPresence }
        if (targets.isEmpty()) return@flow
        while (true) {
            targets.forEach { channel ->
                runCatching {
                    val event = LocalKeySigner(keys.forArea(channel.geohash)).sign(ChannelEvents.presence(channel.geohash, now()))
                    remoteSource.publish(event, GeoRelays.nearest(channel.geohash))
                }
                // Spread over a few seconds, so the areas' heartbeats are not obviously one person.
                delay(Random.nextLong(2_000, 5_000))
            }
            emit(Unit)
            delay(Random.nextLong(40_000, 80_000))
        }
    }

    private suspend fun currentNickname(): String = preferences.channelNickname.first()

    private fun now() = clock.now().epochSeconds

    /** Who was seen where, last time each; times from the future count as now. */
    private class Seen {
        private val lastSeen = mutableMapOf<String, MutableMap<String, Long>>()

        fun record(area: String, pubkey: String, at: Long, now: Long) {
            val people = lastSeen.getOrPut(area) { mutableMapOf() }
            people[pubkey] = maxOf(people[pubkey] ?: 0, minOf(at, now))
        }

        fun counts(areas: List<String>, now: Long): Map<String, Int?> =
            areas.associateWith { area -> lastSeen[area].orEmpty().count { (_, at) -> at >= now - WINDOW_SECONDS } }
    }

    private companion object {
        /** bitchat counts people seen in the last five minutes. */
        const val WINDOW_SECONDS = 5 * 60L
        /** A room opens with the last hour, as bitchat's does. */
        const val HISTORY_SECONDS = 60 * 60L
        const val MAX_MESSAGES = 500
        val COUNT_REFRESH = 30.seconds
        /** bitchat Android's default when its PoW filter is on (iOS mines 8). */
        const val POW_BITS = 12
    }
}
