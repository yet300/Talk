package com.yet.talk.api.schema

import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.nip.Imeta
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.Tags
import com.yet.talk.nostr.nip.reaction
import com.yet.talk.nostr.nip.repost

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
            listOf(parent.author, threadRoot.author).filter { it.isNotEmpty() }.distinct().forEach { add(Tags.pubkey(it)) }
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

    /**
     * A public report (NIP-56 kind 1984) of a post ([eventId] with its [pubkey]) or of a profile ([eventId] null),
     * [type] being one of NIP-56's report types.
     */
    fun report(pubkey: String, eventId: String?, type: String, createdAt: Long) = EventTemplate(
        kind = Kinds.REPORT,
        content = "",
        tags = (if (eventId != null) listOf(listOf("e", eventId, type), listOf("p", pubkey)) else listOf(listOf("p", pubkey, type))) +
            TalkSchema.labels(TalkSchema.Role.REPORT),
        createdAt = createdAt,
    )

    /** A like of a video (NIP-25 kind 17 on the NIP-73 URL). */
    fun videoLike(videoUrl: String, createdAt: Long) = EventTemplate(
        kind = Kinds.EXTERNAL_REACTION,
        content = "+",
        tags = listOf(listOf("k", TalkSchema.WEB), listOf("i", videoUrl)) + TalkSchema.labels(TalkSchema.Role.VIDEO_LIKE),
        createdAt = createdAt,
    )

    /** A top-level comment on a video (NIP-22 kind 1111): root and parent are the video URL. */
    fun videoComment(videoUrl: String, content: String, createdAt: Long) = EventTemplate(
        kind = Kinds.COMMENT,
        content = content,
        tags = listOf(
            listOf("I", videoUrl), listOf("K", TalkSchema.WEB), listOf("i", videoUrl), listOf("k", TalkSchema.WEB),
        ) + TalkSchema.labels(TalkSchema.Role.VIDEO_COMMENT),
        createdAt = createdAt,
    )

    /** NIP-09 deletion of the author's own events, given as id to kind. */
    fun delete(targets: Map<String, Int>, createdAt: Long) = EventTemplate(
        kind = Kinds.DELETION,
        content = "",
        tags = targets.keys.map { Tags.event(it) } + targets.values.distinct().map(Tags::kind) +
            TalkSchema.labels(TalkSchema.Role.DELETION),
        createdAt = createdAt,
    )

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
