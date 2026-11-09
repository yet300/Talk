package com.yet.talk.mcp

import com.yet.talk.api.schema.PostRef
import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.nostr.encoding.Nip19
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.contactList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ReadingTest {
    private val author = SecretKey.generate()
    private val agent = Agent()

    private fun text(value: String) = JsonPrimitive(value)

    private fun JsonObject.posts() = getValue("posts").jsonArray.map { it.jsonObject }

    private fun JsonObject.markdown() = getValue("markdown").jsonPrimitive.content

    private suspend fun post(content: String, createdAt: Long = 1_700_000_000, by: SecretKey = author) =
        agent.relays.seed(by, TalkEvents.post(content, emptyList(), null, createdAt))

    @Test
    fun theFeedShowsTalkPostsWithoutReplies() = runTest {
        val first = post("First", 1_700_000_000)
        post("Second", 1_700_000_100)
        agent.relays.seed(author, TalkEvents.reply("A reply", emptyList(), PostRef(first.id, first.pubkey), null, 1_700_000_200))
        agent.relays.seed(author, EventTemplate(Kinds.TEXT_NOTE, "Not Talk", emptyList(), 1_700_000_300))
        val posts = agent.call("talk_read_feed").json.posts()
        assertEquals(listOf("Second", "First"), posts.map { it.markdown() })
    }

    @Test
    fun theFeedPagesWithUntil() = runTest {
        post("Old", 1_700_000_000)
        post("New", 1_700_000_100)
        val page = agent.call("talk_read_feed", "limit" to JsonPrimitive(1)).json
        assertEquals(listOf("New"), page.posts().map { it.markdown() })
        val next = agent.call("talk_read_feed", "limit" to JsonPrimitive(1), "until" to page.getValue("next_until")).json
        assertEquals(listOf("Old"), next.posts().map { it.markdown() })
    }

    @Test
    fun theFollowingFeedHoldsOnlyFollowedAuthors() = runTest {
        post("Followed")
        post("Stranger", by = SecretKey.generate())
        agent.relays.seed(agent.key, contactList(listOf(author.publicKey), 1_700_000_000))
        val posts = agent.call("talk_read_feed", "feed" to text("following")).json.posts()
        assertEquals(listOf("Followed"), posts.map { it.markdown() })
    }

    @Test
    fun trendingRanksEngagedPosts() = runTest {
        val now = agent.clock.seconds
        val quiet = post("Quiet", now - 100)
        val popular = post("Popular", now - 200)
        agent.relays.seed(agent.key, TalkEvents.like(popular, now - 50))
        val posts = agent.call("talk_read_feed", "feed" to text("trending")).json.posts()
        assertEquals(listOf("Popular"), posts.map { it.markdown() })
        assertFalse(posts.any { it.getValue("id").jsonPrimitive.content == quiet.id })
    }

    @Test
    fun aPostShowsItsNewestRevisionCountsAndReplies() = runTest {
        val original = post("Original")
        agent.relays.seed(author, TalkEvents.revision(original.id, 2, "Edited", emptyList(), 1_700_000_050))
        // Only the author's revisions count.
        agent.relays.seed(SecretKey.generate(), TalkEvents.revision(original.id, 3, "Forged", emptyList(), 1_700_000_060))
        agent.relays.seed(agent.key, TalkEvents.like(original, 1_700_000_070))
        agent.relays.seed(author, TalkEvents.reply("A reply", emptyList(), PostRef(original.id, original.pubkey), null, 1_700_000_080))
        val result = agent.call("talk_read_post", "post_id" to text(Nip19.note(original.id))).json
        val shown = result.getValue("post").jsonObject
        assertEquals("Edited", shown.markdown())
        assertEquals(2, shown.getValue("revision").jsonPrimitive.int)
        assertEquals(1, shown.getValue("likes").jsonPrimitive.int)
        assertEquals(1, shown.getValue("replies").jsonPrimitive.int)
        assertEquals(listOf("A reply"), result.getValue("replies").jsonArray.map { it.jsonObject.markdown() })
    }

    @Test
    fun deletedPostsAreLeftOutButOnlyTheAuthorCanDelete() = runTest {
        val deleted = post("Deleted")
        agent.relays.seed(author, TalkEvents.delete(mapOf(deleted.id to Kinds.TEXT_NOTE), 1_700_000_100))
        val kept = post("Kept")
        agent.relays.seed(SecretKey.generate(), TalkEvents.delete(mapOf(kept.id to Kinds.TEXT_NOTE), 1_700_000_100))
        assertEquals(listOf("Kept"), agent.call("talk_read_feed").json.posts().map { it.markdown() })
    }

    @Test
    fun anUndoneLikeIsNotCounted() = runTest {
        val original = post("Post")
        val fan = SecretKey.generate()
        val like = agent.relays.seed(fan, TalkEvents.like(original, 1_700_000_010))
        agent.relays.seed(agent.key, TalkEvents.like(original, 1_700_000_020))
        agent.relays.seed(fan, TalkEvents.delete(mapOf(like.id to Kinds.REACTION), 1_700_000_030))
        val shown = agent.call("talk_read_post", "post_id" to text(original.id)).json.getValue("post").jsonObject
        assertEquals(1, shown.getValue("likes").jsonPrimitive.int)
    }

    @Test
    fun onlyTheAuthorOfALikeCanUndoIt() = runTest {
        val original = post("Post")
        val like = agent.relays.seed(SecretKey.generate(), TalkEvents.like(original, 1_700_000_010))
        agent.relays.seed(SecretKey.generate(), TalkEvents.delete(mapOf(like.id to Kinds.REACTION), 1_700_000_030))
        val shown = agent.call("talk_read_post", "post_id" to text(original.id)).json.getValue("post").jsonObject
        assertEquals(1, shown.getValue("likes").jsonPrimitive.int)
    }

    @Test
    fun anUndoneRepostIsNotCounted() = runTest {
        val original = post("Post")
        val fan = SecretKey.generate()
        val repost = agent.relays.seed(fan, TalkEvents.repost(original, 1_700_000_010))
        agent.relays.seed(fan, TalkEvents.delete(mapOf(repost.id to Kinds.REPOST), 1_700_000_030))
        val shown = agent.call("talk_read_post", "post_id" to text(original.id)).json.getValue("post").jsonObject
        assertEquals(0, shown.getValue("reposts").jsonPrimitive.int)
    }

    @Test
    fun anUndoneLikeDoesNotMakeAPostTrend() = runTest {
        val now = agent.clock.seconds
        val original = post("Post", now - 100)
        val fan = SecretKey.generate()
        val like = agent.relays.seed(fan, TalkEvents.like(original, now - 50))
        agent.relays.seed(fan, TalkEvents.delete(mapOf(like.id to Kinds.REACTION), now - 10))
        assertTrue(agent.call("talk_read_feed", "feed" to text("trending")).json.posts().isEmpty())
    }

    @Test
    fun trendingHasNoCursor() = runTest {
        val now = agent.clock.seconds
        val popular = post("Popular", now - 200)
        agent.relays.seed(agent.key, TalkEvents.like(popular, now - 50))
        val page = agent.call("talk_read_feed", "feed" to text("trending")).json
        assertEquals(1, page.posts().size)
        assertFalse("next_until" in page)
    }

    @Test
    fun aProfileSaysWhetherItIsABot() = runTest {
        agent.relays.seed(author, EventTemplate(Kinds.METADATA, """{"name":"Scout","bot":true}""", emptyList(), 1_700_000_000))
        post("Mine")
        val result = agent.call("talk_read_profile", "pubkey" to text(Nip19.npub(author.publicKey))).json
        val profile = result.getValue("profile").jsonObject
        assertEquals("Scout", profile.getValue("name").jsonPrimitive.content)
        assertTrue(profile.getValue("bot").jsonPrimitive.content == "true")
        assertEquals(listOf("Mine"), result.getValue("posts").jsonObject.posts().map { it.markdown() })
    }

    @Test
    fun searchFindsPostsAndProfiles() = runTest {
        post("Tiny transformers")
        post("Something else")
        agent.relays.seed(author, EventTemplate(Kinds.METADATA, """{"name":"transformers fan"}""", emptyList(), 1_700_000_000))
        assertEquals(listOf("Tiny transformers"), agent.call("talk_search", "query" to text("transformers")).json.posts().map { it.markdown() })
        val profiles = agent.call("talk_search", "query" to text("transformers"), "type" to text("profiles")).json.getValue("profiles").jsonArray
        assertEquals(1, profiles.size)
    }

    @Test
    fun activityListsWhatTaggedTheAgent() = runTest {
        val mine = post("Mine", by = agent.key)
        agent.relays.seed(author, TalkEvents.reply("Nice", emptyList(), PostRef(mine.id, mine.pubkey), null, 1_700_000_100))
        agent.relays.seed(author, TalkEvents.like(mine, 1_700_000_200))
        val activity = agent.call("talk_read_activity").json.getValue("activity").jsonArray.map { it.jsonObject }
        assertEquals(listOf("like", "reply"), activity.map { it.getValue("type").jsonPrimitive.content })
    }

    @Test
    fun malformedIdsAreExplained() = runTest {
        assertTrue(agent.call("talk_read_post", "post_id" to text("nope")).isError)
        assertTrue(agent.call("talk_read_profile", "pubkey" to text("nope")).isError)
    }
}
