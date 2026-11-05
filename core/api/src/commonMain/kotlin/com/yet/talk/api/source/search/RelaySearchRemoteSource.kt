package com.yet.talk.api.source.search

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.schema.TalkSchema
import com.yet.talk.api.source.relay.DefaultRelays
import com.yet.talk.nostr.message.Filter
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.relay.RelayClient
import dev.zacsweers.metro.Inject

@Inject
internal class RelaySearchRemoteSource(private val relays: RelayClient) : SearchRemoteSource {

    override suspend fun searchPosts(query: String, limit: Int): List<TalkEvent.Post> =
        query(Filter(kinds = listOf(Kinds.TEXT_NOTE), tags = mapOf('L' to listOf(TalkSchema.NAMESPACE)), search = query, limit = limit))
            .filterIsInstance<TalkEvent.Post>()

    override suspend fun searchProfiles(query: String, limit: Int): List<TalkEvent.Profile> =
        query(Filter(kinds = listOf(Kinds.METADATA), search = query, limit = limit)).filterIsInstance<TalkEvent.Profile>()

    private suspend fun query(filter: Filter): List<TalkEvent> =
        relays.query(listOf(filter), DefaultRelays.searchUrls).mapNotNull(TalkEvent::parse)
}
