package com.yet.talk.mcp.talk

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.source.post.PostRemoteSource

/**
 * The likes and reposts in [related] that still stand. Undoing one is a NIP-09 deletion of the reaction, which names
 * the reaction and not the post, so it takes a second query; only the reaction's own author can undo it.
 */
internal suspend fun PostRemoteSource.standingReactions(related: List<TalkEvent>): List<TalkEvent.Reaction> {
    val reactions = related.filterIsInstance<TalkEvent.Reaction>()
    val authorOf = reactions.associate { it.event.id to it.event.pubkey }
    val undone = related(authorOf.keys).filterIsInstance<TalkEvent.Deletion>()
        .flatMap { deletion -> deletion.eventIds.filter { authorOf[it] == deletion.event.pubkey } }
        .toSet()
    return reactions.filter { it.event.id !in undone }
}
