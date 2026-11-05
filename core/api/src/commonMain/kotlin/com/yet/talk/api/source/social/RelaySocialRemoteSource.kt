package com.yet.talk.api.source.social

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.schema.TalkSchema
import com.yet.talk.api.source.relay.FILTER_CHUNK
import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.nostr.message.Filter
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.relay.RelayClient
import dev.zacsweers.metro.Inject

@Inject
internal class RelaySocialRemoteSource(
    private val relays: RelayClient,
    private val selection: RelaySelection,
) : SocialRemoteSource {

    override suspend fun profiles(pubkeys: Collection<String>): List<TalkEvent.Profile> {
        if (pubkeys.isEmpty()) return emptyList()
        val filters = pubkeys.distinct().chunked(FILTER_CHUNK).map { Filter(kinds = listOf(Kinds.METADATA), authors = it) }
        return query(filters).filterIsInstance<TalkEvent.Profile>()
    }

    override suspend fun lists(pubkey: String): List<TalkEvent> =
        query(listOf(Filter(kinds = listOf(Kinds.CONTACTS, Kinds.BOOKMARKS, Kinds.RELAY_LIST), authors = listOf(pubkey))))
            .groupBy { it.event.kind }
            .map { (_, versions) -> versions.maxBy { it.event.createdAt } }

    override suspend fun followers(pubkey: String, limit: Int): List<TalkEvent.Contacts> =
        query(listOf(Filter(kinds = listOf(Kinds.CONTACTS), tags = mapOf('p' to listOf(pubkey)), limit = limit)))
            .filterIsInstance<TalkEvent.Contacts>()
            .filter { pubkey in it.pubkeys }

    override suspend fun activity(pubkey: String, until: Long?, limit: Int): List<TalkEvent> {
        val mentions = mapOf('p' to listOf(pubkey))
        return query(
            listOf(
                Filter(kinds = listOf(Kinds.TEXT_NOTE), tags = mentions + ('L' to listOf(TalkSchema.NAMESPACE)), until = until, limit = limit),
                Filter(kinds = listOf(Kinds.REACTION, Kinds.REPOST, Kinds.GENERIC_REPOST), tags = mentions, until = until, limit = limit),
            ),
        ).filter { it.event.pubkey != pubkey }
    }

    override suspend fun countFollowers(pubkey: String): Long? =
        relays.count(listOf(Filter(kinds = listOf(Kinds.CONTACTS), tags = mapOf('p' to listOf(pubkey)))), selection.readRelays())

    override suspend fun countPosts(pubkey: String): Long? =
        relays.count(
            listOf(Filter(kinds = listOf(Kinds.TEXT_NOTE), authors = listOf(pubkey), tags = mapOf('L' to listOf(TalkSchema.NAMESPACE)))),
            selection.readRelays(),
        )

    private suspend fun query(filters: List<Filter>): List<TalkEvent> =
        relays.query(filters, selection.readRelays()).mapNotNull(TalkEvent::parse)
}
