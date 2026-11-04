package com.yet.talk.api.source

import com.yet.talk.api.TalkEvent
import com.yet.talk.api.TalkSchema
import com.yet.talk.nostr.Filter
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.relay.RelayClient
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/** Talk posts and everything attached to them, read from relays. Results are verified and parsed. */
interface PostRemoteSource {
    /** Labelled kind 1 events (posts and replies) older than or at [until], newest first. */
    suspend fun timeline(authors: List<String>?, until: Long?, limit: Int): List<TalkEvent.Post>

    /** Labelled posts created after [since]: the trending window. */
    suspend fun since(since: Long, limit: Int): List<TalkEvent.Post>

    suspend fun byIds(ids: Collection<String>): List<TalkEvent.Post>

    /** Every labelled reply in the thread started by [rootId] (NIP-10 replies all tag the root). */
    suspend fun thread(rootId: String): List<TalkEvent.Post>

    /** Revisions, likes, reposts, replies and deletions that point at [postIds]. */
    suspend fun related(postIds: Collection<String>): List<TalkEvent>
}

@ContributesBinding(AppScope::class)
@Inject
class RelayPostRemoteSource(
    private val relays: RelayClient,
    private val selection: RelaySelection,
) : PostRemoteSource {

    override suspend fun timeline(authors: List<String>?, until: Long?, limit: Int): List<TalkEvent.Post> {
        val chunks = authors?.chunked(FILTER_CHUNK)?.takeIf { it.isNotEmpty() } ?: if (authors == null) listOf(null) else return emptyList()
        val filters = chunks.map { Filter(kinds = listOf(Kinds.TEXT_NOTE), authors = it, tags = LABEL, until = until, limit = limit) }
        return query(filters).filterIsInstance<TalkEvent.Post>().take(limit)
    }

    override suspend fun since(since: Long, limit: Int): List<TalkEvent.Post> =
        query(listOf(Filter(kinds = listOf(Kinds.TEXT_NOTE), tags = LABEL, since = since, limit = limit)))
            .filterIsInstance<TalkEvent.Post>()

    override suspend fun byIds(ids: Collection<String>): List<TalkEvent.Post> =
        query(ids.chunked(FILTER_CHUNK).map { Filter(ids = it, kinds = listOf(Kinds.TEXT_NOTE)) }).filterIsInstance<TalkEvent.Post>()

    override suspend fun thread(rootId: String): List<TalkEvent.Post> =
        query(listOf(Filter(kinds = listOf(Kinds.TEXT_NOTE), tags = LABEL + ('e' to listOf(rootId))))).filterIsInstance<TalkEvent.Post>()

    override suspend fun related(postIds: Collection<String>): List<TalkEvent> {
        if (postIds.isEmpty()) return emptyList()
        val filters = postIds.chunked(FILTER_CHUNK).flatMap { ids ->
            val references = mapOf('e' to ids)
            listOf(
                Filter(kinds = listOf(TalkSchema.KIND_REVISION, Kinds.TEXT_NOTE), tags = references + LABEL),
                Filter(kinds = listOf(Kinds.REACTION, Kinds.REPOST, Kinds.GENERIC_REPOST, Kinds.DELETION), tags = references),
            )
        }
        return query(filters)
    }

    private suspend fun query(filters: List<Filter>): List<TalkEvent> =
        relays.query(filters, selection.readRelays()).mapNotNull(TalkEvent::parse)

    private companion object {
        val LABEL = mapOf('L' to listOf(TalkSchema.NAMESPACE))
    }
}
