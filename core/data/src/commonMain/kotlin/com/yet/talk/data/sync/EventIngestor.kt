package com.yet.talk.data.sync

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.data.paging.FeedKeys
import com.yet.talk.data.mapper.toImagesJson
import com.yet.talk.database.dao.BookmarkDao
import com.yet.talk.database.dao.FollowDao
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.dao.ProfileDao
import com.yet.talk.database.dao.ReactionDao
import com.yet.talk.database.dao.RelayListDao
import com.yet.talk.database.dao.SyncDao
import com.yet.talk.database.dao.Transactor
import com.yet.talk.database.model.NewPostRecord
import com.yet.talk.database.model.ProfileRecord
import com.yet.talk.nostr.nip.externalIdentities
import com.yet.talk.database.model.PublishStates
import com.yet.talk.database.model.ReactionRecord
import com.yet.talk.database.model.ReactionTypes
import com.yet.talk.database.model.RelayRecord
import com.yet.talk.database.model.RevisionRecord
import dev.zacsweers.metro.Inject

/**
 * Writes verified relay events into the database, enforcing who may do what: revisions and
 * deletions only count for the author's own events.
 */
@Inject
internal class EventIngestor(
    private val posts: PostDao,
    private val profiles: ProfileDao,
    private val reactions: ReactionDao,
    private val follows: FollowDao,
    private val bookmarks: BookmarkDao,
    private val relayLists: RelayListDao,
    private val syncDao: SyncDao,
    private val transactor: Transactor,
) {
    /**
     * Returns how many posts were new. One transaction for the whole batch. Posts go first so
     * revisions can be checked against their author.
     */
    suspend fun ingest(events: List<TalkEvent>, publishState: String = PublishStates.PUBLISHED): Int =
        if (events.isEmpty()) 0 else transactor.inTransaction { store(events, publishState) }

    private suspend fun store(events: List<TalkEvent>, publishState: String): Int {
        var newPosts = 0
        events.filterIsInstance<TalkEvent.Post>().forEach { if (ingestPost(it, publishState)) newPosts++ }
        events.forEach { event ->
            when (event) {
                is TalkEvent.Post -> Unit
                is TalkEvent.Revision -> ingestRevision(event)
                is TalkEvent.Reaction -> reactions.insert(
                    ReactionRecord(
                        eventId = event.event.id,
                        postId = event.postId,
                        user = event.event.pubkey,
                        type = if (event.type == TalkEvent.Reaction.Type.Like) ReactionTypes.LIKE else ReactionTypes.REPOST,
                        createdAt = event.event.createdAt,
                    ),
                )
                is TalkEvent.Profile -> with(event.metadata) {
                    val github = event.event.tags.externalIdentities().firstOrNull { it.platform == "github" }
                    profiles.upsert(
                        ProfileRecord(
                            event.event.pubkey, name, displayName, about, picture, website, nip05, event.event.createdAt, banner,
                            githubLogin = github?.identity, githubProof = github?.proof,
                        ),
                    )
                }
                is TalkEvent.Deletion -> {
                    posts.markDeleted(event.eventIds, author = event.event.pubkey)
                    reactions.delete(event.eventIds, user = event.event.pubkey)
                }
                // Someone else is followed now: the Following feed's remote cursor no longer covers them.
                is TalkEvent.Contacts -> if (follows.replace(event.event.pubkey, event.pubkeys, event.event.createdAt)) {
                    syncDao.reset(FeedKeys.following(event.event.pubkey))
                }
                is TalkEvent.Bookmarks -> bookmarks.replace(event.event.pubkey, event.eventIds, event.event.createdAt)
                is TalkEvent.RelayList -> relayLists.replace(
                    event.event.pubkey,
                    event.relays.map { RelayRecord(it.url, it.read, it.write) },
                    event.event.createdAt,
                )
            }
        }
        return newPosts
    }

    private suspend fun ingestPost(post: TalkEvent.Post, publishState: String): Boolean {
        val event = post.event
        val reply = post.thread.isReply
        val inserted = posts.insert(
            NewPostRecord(
                id = event.id,
                author = event.pubkey,
                rootId = if (reply) post.thread.root ?: post.thread.parent else null,
                parentId = post.thread.parent,
                quoteId = post.quoteId,
                createdAt = event.createdAt,
                content = event.content,
                imagesJson = post.images.toImagesJson(),
                publishState = publishState,
                eventJson = event.toJson(),
            ),
        )
        if (inserted) {
            posts.addRevision(RevisionRecord(event.id, event.id, 1, event.content, post.images.toImagesJson(), event.createdAt))
        } else if (publishState == PublishStates.PUBLISHED) {
            // Our own pending post came back from a relay: it is published.
            posts.setPublishState(event.id, PublishStates.PUBLISHED)
        }
        return inserted
    }

    private suspend fun ingestRevision(revision: TalkEvent.Revision) {
        if (posts.authorOf(revision.postId) != revision.event.pubkey) return
        posts.addRevision(
            RevisionRecord(
                id = revision.event.id,
                postId = revision.postId,
                number = revision.number,
                content = revision.event.content,
                imagesJson = revision.images.toImagesJson(),
                createdAt = revision.event.createdAt,
            ),
        )
    }
}
