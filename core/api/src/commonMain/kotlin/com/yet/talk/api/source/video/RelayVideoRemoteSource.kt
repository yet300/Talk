package com.yet.talk.api.source.video

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.source.relay.RelaySelection
import com.yet.talk.nostr.message.Filter
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.relay.RelayClient
import dev.zacsweers.metro.Inject

@Inject
internal class RelayVideoRemoteSource(
    private val relays: RelayClient,
    private val selection: RelaySelection,
) : VideoRemoteSource {
    override suspend fun activity(videoUrl: String): List<TalkEvent> {
        val filters = listOf(
            Filter(kinds = listOf(Kinds.EXTERNAL_REACTION), tags = mapOf('i' to listOf(videoUrl))),
            Filter(kinds = listOf(Kinds.COMMENT), tags = mapOf('I' to listOf(videoUrl))),
        )
        return relays.query(filters, selection.readRelays()).mapNotNull(TalkEvent::parse)
    }
}
