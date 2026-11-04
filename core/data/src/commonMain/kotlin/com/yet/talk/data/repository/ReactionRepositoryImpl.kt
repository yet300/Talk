package com.yet.talk.data.repository

import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.api.source.post.PostRemoteSource
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.dao.ReactionDao
import com.yet.talk.database.model.ReactionTypes
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.repository.ReactionRepository
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.nip.Kinds
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class ReactionRepositoryImpl(
    private val posts: PostDao,
    private val reactions: ReactionDao,
    private val postsRemote: PostRemoteSource,
    private val sync: RemoteSync,
    private val publisher: Publisher,
) : ReactionRepository {

    override suspend fun setLiked(postId: PostId, liked: Boolean): Result<Unit> =
        set(postId, ReactionTypes.LIKE, liked) { target, createdAt -> TalkEvents.like(target, createdAt) }

    override suspend fun setReposted(postId: PostId, reposted: Boolean): Result<Unit> =
        set(postId, ReactionTypes.REPOST, reposted) { target, createdAt -> TalkEvents.repost(target, createdAt) }

    private suspend fun set(
        postId: PostId,
        type: String,
        active: Boolean,
        build: (Event, Long) -> EventTemplate,
    ): Result<Unit> = remote {
        val me = publisher.me()
        val existing = reactions.reactionIds(postId.value, me, type)
        if (active) {
            if (existing.isNotEmpty()) return@remote
            val target = targetEvent(postId)
            val event = publisher.sign { build(target, it) }
            try {
                publisher.storeAndPublish(event)
            } catch (error: Throwable) {
                reactions.delete(listOf(event.id), me) // roll back the optimistic like
                throw error
            }
        } else {
            if (existing.isEmpty()) return@remote
            val kind = if (type == ReactionTypes.LIKE) Kinds.REACTION else Kinds.REPOST
            publisher.publish(publisher.sign { TalkEvents.delete(existing.associateWith { kind }, it) })
            reactions.delete(existing, me)
        }
    }

    private suspend fun targetEvent(postId: PostId): Event {
        posts.eventJson(postId.value)?.let { return Event.fromJson(it) }
        sync.storeWithContext(postsRemote.byIds(listOf(postId.value)))
        return posts.eventJson(postId.value)?.let(Event::fromJson) ?: throw DomainError.NotFound
    }
}
