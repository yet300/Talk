package com.yet.talk.api.source.search

import com.yet.talk.api.schema.TalkEvent

/** NIP-50 full-text search on relays that support it. */
interface SearchRemoteSource {
    /** Talk posts and replies matching [query]. */
    suspend fun searchPosts(query: String, limit: Int): List<TalkEvent.Post>

    /** Profiles whose metadata matches [query]. */
    suspend fun searchProfiles(query: String, limit: Int): List<TalkEvent.Profile>
}
