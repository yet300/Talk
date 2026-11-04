package com.yet.talk.api

import com.yet.talk.api.schema.PostRef
import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.api.schema.TalkSchema
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.nip.Imeta
import com.yet.talk.nostr.signer.LocalKeySigner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class TalkEventsTest {

    private val alice = LocalKeySigner(SecretKey.generate())
    private val bob = LocalKeySigner(SecretKey.generate())

    @Test
    fun postRoundTripsWithLabelImagesAndQuote() = runTest {
        val quoted = alice.sign(TalkEvents.post("original", emptyList(), null, createdAt = 1))
        val image = Imeta(url = "https://b/x.png", mimeType = "image/png", sha256 = "ab")
        val event = bob.sign(TalkEvents.post("## hi", listOf(image), PostRef(quoted.id, quoted.pubkey), createdAt = 2))

        val post = assertIs<TalkEvent.Post>(TalkEvent.parse(event))
        assertTrue(TalkSchema.isTalkEvent(event))
        assertEquals(false, post.thread.isReply)
        assertEquals(quoted.id, post.quoteId)
        assertEquals(listOf(image), post.images)
        assertTrue(listOf("p", quoted.pubkey) in event.tags)
    }

    @Test
    fun nestedReplyMarksRootAndParent() = runTest {
        val root = alice.sign(TalkEvents.post("root", emptyList(), null, 1))
        val direct = bob.sign(TalkEvents.reply("direct", emptyList(), PostRef(root.id, root.pubkey), root = null, createdAt = 2))
        val nested = alice.sign(TalkEvents.reply("nested", emptyList(), PostRef(direct.id, direct.pubkey, "rev"), PostRef(root.id, root.pubkey), 3))

        val directThread = assertIs<TalkEvent.Post>(TalkEvent.parse(direct)).thread
        assertEquals(root.id, directThread.root)
        assertEquals(root.id, directThread.parent)

        val nestedThread = assertIs<TalkEvent.Post>(TalkEvent.parse(nested)).thread
        assertEquals(root.id, nestedThread.root)
        assertEquals(direct.id, nestedThread.parent)
        assertTrue(listOf("revision", "rev") in nested.tags)

        val unknownRootAuthor = alice.sign(TalkEvents.reply("x", emptyList(), PostRef(direct.id, direct.pubkey), PostRef(root.id, ""), 4))
        assertTrue(unknownRootAuthor.tags.none { it == listOf("p") || it == listOf("p", "") })
    }

    @Test
    fun revisionsAndReactionsParse() = runTest {
        val post = alice.sign(TalkEvents.post("v1", emptyList(), null, 1))
        val revision = assertIs<TalkEvent.Revision>(TalkEvent.parse(alice.sign(TalkEvents.revision(post.id, 2, "v2", emptyList(), 5))))
        assertEquals(post.id, revision.postId)
        assertEquals(2, revision.number)

        val like = assertIs<TalkEvent.Reaction>(TalkEvent.parse(bob.sign(TalkEvents.like(post, 6))))
        assertEquals(TalkEvent.Reaction.Type.Like to post.id, like.type to like.postId)

        val repost = assertIs<TalkEvent.Reaction>(TalkEvent.parse(bob.sign(TalkEvents.repost(post, 7))))
        assertEquals(TalkEvent.Reaction.Type.Repost, repost.type)

        val deletion = assertIs<TalkEvent.Deletion>(TalkEvent.parse(alice.sign(TalkEvents.delete(mapOf(post.id to post.kind), 8))))
        assertEquals(listOf(post.id), deletion.eventIds)
    }

    @Test
    fun ignoresEventsOutsideTheCommunity() = runTest {
        val foreign = alice.sign(EventTemplate(kind = 1, content = "hello nostr", createdAt = 1))
        assertNull(TalkEvent.parse(foreign))
        val dislike = bob.sign(EventTemplate(kind = 7, content = "-", tags = listOf(listOf("e", foreign.id)), createdAt = 2))
        assertNull(TalkEvent.parse(dislike))
    }
}
