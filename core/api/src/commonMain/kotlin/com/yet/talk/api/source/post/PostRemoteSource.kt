package com.yet.talk.api.source.post

import com.yet.talk.api.schema.TalkEvent

/** Talk posts and everything attached to them, read from relays. Results are verified and parsed. */
interface PostRemoteSource {
    /** Labelled kind 1 events (posts and replies) older than or at [until], newest first. */
    suspend fun timeline(authors: List<String>?, until: Long?, limit: Int): List<TalkEvent.Post>

    /** Labelled posts created after [since]: the trending window. */
    /** Talk reposts (kind 6) by [authors], newest first, before [until]. */
    suspend fun reposts(authors: List<String>, until: Long?, limit: Int): List<TalkEvent.Reaction>

    suspend fun since(since: Long, limit: Int): List<TalkEvent.Post>

    suspend fun byIds(ids: Collection<String>): List<TalkEvent.Post>

    /** Every labelled reply in the thread started by [rootId] (NIP-10 replies all tag the root). */
    suspend fun thread(rootId: String): List<TalkEvent.Post>

    /** Revisions, likes, reposts, replies and deletions that point at [postIds]. */
    suspend fun related(postIds: Collection<String>): List<TalkEvent>
}
