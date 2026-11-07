package com.yet.talk.api.source.social

import com.yet.talk.api.schema.TalkEvent

/** Profiles, replaceable lists and what other people did to a user, read from relays. */
interface SocialRemoteSource {
    suspend fun profiles(pubkeys: Collection<String>): List<TalkEvent.Profile>

    /** The newest contact, bookmark and relay lists of [pubkey] that relays return. */
    suspend fun lists(pubkey: String): List<TalkEvent>

    /** Contact lists that include [pubkey]: its followers. */
    suspend fun followers(pubkey: String, limit: Int): List<TalkEvent.Contacts>

    /** Talk replies and quotes, likes and reposts that tag [pubkey], older than or at [until]. */
    suspend fun activity(pubkey: String, until: Long?, limit: Int): List<TalkEvent>

    /** NIP-45 counts; null when no relay supports them. */
    /** NIP-56 reports made by [authors], newest first. */
    suspend fun reports(authors: Collection<String>, limit: Int): List<TalkEvent.Report>

    suspend fun countFollowers(pubkey: String): Long?

    suspend fun countPosts(pubkey: String): Long?
}
