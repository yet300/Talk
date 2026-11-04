package com.yet.talk.api

import com.yet.talk.nostr.Event
import com.yet.talk.nostr.EventTemplate
import com.yet.talk.nostr.nip.Imeta
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.Tags
import com.yet.talk.nostr.nip.deletion
import com.yet.talk.nostr.nip.reaction
import com.yet.talk.nostr.nip.repost

/** A post as other events point at it: its id, its author and, if it was edited, the revision that was seen. */
data class PostRef(val id: String, val author: String, val revisionId: String? = null)

/** Builds unsigned Talk events. Every builder takes `createdAt` (epoch seconds) so callers own the clock. */
object TalkEvents {

    fun post(content: String, images: List<Imeta>, quote: PostRef?, createdAt: Long) = EventTemplate(
        kind = Kinds.TEXT_NOTE,
        content = content,
        tags = TalkSchema.labels(TalkSchema.Role.POST) + images.map(Imeta::toTag) + quote.quoteTags(),
        createdAt = createdAt,
    )

    /** A NIP-10 reply: `root` is the thread's first post, `reply` the direct parent when that is not the root. */
    fun reply(content: String, images: List<Imeta>, parent: PostRef, root: PostRef?, createdAt: Long): EventTemplate {
        val threadRoot = root ?: parent
        val threadTags = buildList {
            add(Tags.event(threadRoot.id, marker = "root", author = threadRoot.author))
            if (threadRoot.id != parent.id) add(Tags.event(parent.id, marker = "reply", author = parent.author))
            listOf(parent.author, threadRoot.author).distinct().forEach { add(Tags.pubkey(it)) }
            parent.revisionId?.let { add(listOf(TalkSchema.REVISION_TAG, it)) }
        }
        return EventTemplate(
            kind = Kinds.TEXT_NOTE,
            content = content,
            tags = TalkSchema.labels(TalkSchema.Role.REPLY) + threadTags + images.map(Imeta::toTag),
            createdAt = createdAt,
        )
    }

    /** Revision [number] (2 or more) of [postId]; the original post is revision 1. */
    fun revision(postId: String, number: Int, content: String, images: List<Imeta>, createdAt: Long): EventTemplate {
        require(number >= 2) { "Revision 1 is the original post" }
        return EventTemplate(
            kind = TalkSchema.KIND_REVISION,
            content = content,
            tags = listOf(Tags.event(postId, marker = "root"), listOf(TalkSchema.REVISION_TAG, number.toString())) +
                TalkSchema.labels(TalkSchema.Role.REVISION) + images.map(Imeta::toTag),
            createdAt = createdAt,
        )
    }

    fun like(target: Event, createdAt: Long) =
        reaction(target, createdAt, extraTags = TalkSchema.labels(TalkSchema.Role.REACTION))

    fun repost(target: Event, createdAt: Long) =
        repost(target, createdAt, extraTags = TalkSchema.labels(TalkSchema.Role.REPOST))

    fun delete(events: List<Event>, createdAt: Long) = deletion(events, createdAt).let {
        it.copy(tags = it.tags + TalkSchema.labels(TalkSchema.Role.DELETION))
    }

    private fun PostRef?.quoteTags(): List<List<String>> = if (this == null) {
        emptyList()
    } else {
        listOfNotNull(
            Tags.quote(id, author = author),
            Tags.pubkey(author),
            revisionId?.let { listOf(TalkSchema.REVISION_TAG, it) },
        )
    }
}
