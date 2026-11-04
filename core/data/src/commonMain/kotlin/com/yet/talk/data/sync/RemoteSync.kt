package com.yet.talk.data.sync

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.source.post.PostRemoteSource
import com.yet.talk.api.source.social.SocialRemoteSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** Fetches what a page of posts needs to render and stores it. */
@SingleIn(AppScope::class)
@Inject
class RemoteSync(
    private val postsRemote: PostRemoteSource,
    private val socialRemote: SocialRemoteSource,
    private val ingestor: EventIngestor,
) {
    /**
     * Stores [posts] with their revisions, likes, reposts, replies, deletions, quoted posts and
     * authors' profiles. Returns how many of [posts] were new.
     */
    suspend fun storeWithContext(posts: List<TalkEvent.Post>): Int {
        if (posts.isEmpty()) return 0
        val quoted = posts.mapNotNull { it.quoteId }.distinct().let { postsRemote.byIds(it) }
        val all = posts + quoted
        val related = postsRemote.related(all.map { it.event.id })
        val replyAuthors = related.filterIsInstance<TalkEvent.Post>().map { it.event.pubkey }
        val profiles = socialRemote.profiles((all.map { it.event.pubkey } + replyAuthors).toSet())
        val newPosts = ingestor.ingest(posts)
        ingestor.ingest(quoted + related + profiles)
        return newPosts
    }

    private val listsSynced = mutableSetOf<String>()

    /** The user's contact, bookmark and relay lists. */
    suspend fun syncLists(pubkey: String) {
        ingestor.ingest(socialRemote.lists(pubkey))
        listsSynced += pubkey
    }

    /**
     * Before changing a replaceable list, have the newest one from relays: publishing a list built
     * from a stale copy would drop entries added on another device.
     */
    suspend fun ensureListsSynced(pubkey: String) {
        if (pubkey !in listsSynced) syncLists(pubkey)
    }
}
