package com.yet.talk.mcp

import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.mcp.agent.Disclosure
import com.yet.talk.mcp.config.RateLimit
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.event.EventVerifier
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.contactList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PublishingTest {
    private val other = SecretKey.generate()

    private fun text(value: String) = JsonPrimitive(value)

    private fun Agent.events(kind: Int) = relays.published.map { it.first }.filter { it.kind == kind }

    private suspend fun FakeRelays.seedPost(content: String = "A post", createdAt: Long = 1_700_000_000) =
        seed(other, TalkEvents.post(content, emptyList(), null, createdAt))

    @Test
    fun aDryRunSendsNothingAndUsesNoBudget() = runTest {
        val agent = Agent(publishEnabled = false)
        repeat(3) {
            val outcome = agent.call("talk_publish_post", "markdown" to text("# Hello"))
            assertFalse(outcome.isError)
            assertTrue(outcome.json.getValue("dry_run").jsonPrimitive.boolean)
            assertFalse(outcome.json.getValue("published").jsonPrimitive.boolean)
            assertEquals(0L, outcome.json.getValue("retry_after_seconds").jsonPrimitive.content.toLong())
        }
        assertTrue(agent.relays.published.isEmpty())
    }

    @Test
    fun everyWriteToolIsADryRunByDefault() = runTest {
        val relays = FakeRelays()
        val post = relays.seedPost()
        val agent = Agent(relays = relays, publishEnabled = false)
        agent.call("talk_reply", "post_id" to text(post.id), "markdown" to text("Hi"))
        agent.call("talk_quote", "post_id" to text(post.id), "markdown" to text("Hi"))
        agent.call("talk_like", "post_id" to text(post.id))
        agent.call("talk_follow", "pubkey" to text(other.publicKey))
        agent.call("talk_set_profile", "about" to text("Reads"))
        assertTrue(relays.published.isEmpty())
    }

    @Test
    fun aDryRunShowsTheTalkLabels() = runTest {
        val outcome = Agent(publishEnabled = false).call("talk_publish_post", "markdown" to text("# Hello"))
        val tags = outcome.json.getValue("would_publish").jsonObject.getValue("tags").toString()
        assertTrue("""["L","com.yet.talk"]""" in tags)
        assertTrue("""["l","post","com.yet.talk"]""" in tags)
    }

    @Test
    fun theFirstPublicationIsPrecededByADisclosingProfile() = runTest {
        val agent = Agent(model = "Claude Opus 5.5")
        val outcome = agent.call("talk_publish_post", "markdown" to text("# Hello"))
        assertFalse(outcome.isError, outcome.text)
        val kinds = agent.relays.published.map { it.first.kind }
        assertEquals(listOf(Kinds.METADATA, Kinds.TEXT_NOTE), kinds)
        val profile = Json.parseToJsonElement(agent.events(Kinds.METADATA).single().content).jsonObject
        assertTrue(profile.getValue("bot").jsonPrimitive.boolean)
        assertTrue(Disclosure.isDisclosed(profile.toString()))
    }

    @Test
    fun aPostFollowsTheSchemaAndIsSigned() = runTest {
        val agent = Agent()
        val outcome = agent.call("talk_publish_post", "markdown" to text("# Hello\r\nworld"))
        assertTrue(outcome.json.getValue("published").jsonPrimitive.boolean)
        val post = agent.events(Kinds.TEXT_NOTE).single()
        assertTrue(EventVerifier.verify(post))
        assertEquals("# Hello\nworld", post.content)
        assertTrue(post.has("L", "com.yet.talk"))
        assertTrue(post.has("l", "post", "com.yet.talk"))
        assertEquals(TEST_RELAYS.toSet(), agent.relays.published.last().second)
    }

    @Test
    fun aKeyThatIsSomeonesAccountCanNeitherPublishNorBeRelabelled() = runTest {
        val agent = Agent()
        agent.relays.seed(agent.key, EventTemplate(Kinds.METADATA, """{"name":"A person"}""", emptyList(), 1_700_000_000))
        val refused = agent.call("talk_publish_post", "markdown" to text("# Hello"))
        assertTrue(refused.isError)
        assertTrue("its own key" in refused.text, refused.text)
        agent.advance(1_000)
        assertTrue(agent.call("talk_set_profile", "about" to text("Reads")).isError)
        assertTrue(agent.relays.published.isEmpty())
    }

    @Test
    fun aDryRunOfRelabellingSomeonesAccountIsRefusedToo() = runTest {
        val agent = Agent(publishEnabled = false)
        agent.relays.seed(agent.key, EventTemplate(Kinds.METADATA, """{"name":"A person"}""", emptyList(), 1_700_000_000))
        assertTrue(agent.call("talk_set_profile", "about" to text("Reads")).isError)
    }

    @Test
    fun aDeclaredAgentCanStillEditItsProfile() = runTest {
        val agent = Agent()
        agent.call("talk_publish_post", "markdown" to text("# Hello"))
        agent.advance(5)
        assertFalse(agent.call("talk_set_profile", "about" to text("Reads")).isError)
        assertTrue("Reads" in agent.events(Kinds.METADATA).last().content)
    }

    @Test
    fun anOutageIsNotTakenForANewKey() = runTest {
        val agent = Agent()
        agent.relays.silent = TEST_RELAYS.toSet()
        val outcome = agent.call("talk_publish_post", "markdown" to text("# Hello"))
        assertTrue(outcome.isError, outcome.text)
        assertTrue(agent.relays.published.isEmpty())

        agent.relays.silent = emptySet()
        assertFalse(agent.call("talk_publish_post", "markdown" to text("# Hello")).isError)
    }

    @Test
    fun aNewKeyNeedsEveryRelayToAnswer() = runTest {
        val urls = listOf("wss://a.test", "wss://b.test")
        val agent = Agent(urls = urls)
        agent.relays.silent = setOf("wss://b.test")
        val outcome = agent.call("talk_publish_post", "markdown" to text("# Hello"))
        assertTrue(outcome.isError)
        assertTrue("wss://b.test" in outcome.text)
        assertFalse("wss://a.test" in outcome.text)
        assertTrue(agent.relays.published.isEmpty())
    }

    @Test
    fun anOutageDoesNotReplaceAFollowListEither() = runTest {
        val agent = Agent()
        agent.relays.seed(agent.key, EventTemplate(Kinds.METADATA, """{"name":"x","bot":true,"about":"${Disclosure.ABOUT_LINE}"}""", emptyList(), 1_700_000_000))
        agent.relays.silent = TEST_RELAYS.toSet()
        assertTrue(agent.call("talk_follow", "pubkey" to text(other.publicKey)).isError)
        assertTrue(agent.relays.published.isEmpty())
    }

    @Test
    fun aProfileThatWasThereIsNotOverwrittenWhenRelaysReturnNone() = runTest {
        val agent = Agent()
        agent.call("talk_publish_post", "markdown" to text("# Hello"))
        val fresh = Agent(relays = FakeRelays(), clock = agent.clock, key = agent.key, store = agent.store)
        agent.advance(1_000)
        val outcome = fresh.call("talk_publish_post", "markdown" to text("# Hello"))
        assertTrue(outcome.isError, outcome.text)
        assertTrue(fresh.relays.published.isEmpty())
    }

    @Test
    fun aNewProfileIsNewerThanTheOneItReplaces() = runTest {
        val agent = Agent()
        val future = agent.clock.seconds + 100
        agent.relays.seed(agent.key, EventTemplate(Kinds.METADATA, """{"name":"x","bot":true,"about":"${Disclosure.ABOUT_LINE}"}""", emptyList(), future))
        agent.call("talk_set_profile", "about" to text("Reads"))
        assertEquals(future + 1, agent.events(Kinds.METADATA).single().createdAt)
    }

    @Test
    fun postsMustKeepTheirDistance() = runTest {
        val agent = Agent(posts = RateLimit(2, 300))
        assertFalse(agent.call("talk_publish_post", "markdown" to text("one")).isError)
        val tooSoon = agent.call("talk_publish_post", "markdown" to text("two"))
        assertTrue(tooSoon.isError)
        assertTrue("Rate limit" in tooSoon.text)
        agent.advance(300)
        assertFalse(agent.call("talk_publish_post", "markdown" to text("two")).isError)
        agent.advance(300)
        assertTrue(agent.call("talk_publish_post", "markdown" to text("three")).isError)
        assertEquals(2, agent.events(Kinds.TEXT_NOTE).size)
    }

    @Test
    fun aRestartDoesNotResetTheLimit() = runTest {
        val agent = Agent()
        agent.call("talk_publish_post", "markdown" to text("one"))
        val restarted = Agent(relays = agent.relays, clock = agent.clock, key = agent.key, store = agent.store)
        assertTrue(restarted.call("talk_publish_post", "markdown" to text("two")).isError)
    }

    @Test
    fun aRefusedPublicationStillCountsAgainstTheLimit() = runTest {
        val agent = Agent()
        agent.relays.seed(agent.key, EventTemplate(Kinds.METADATA, """{"name":"x","bot":true,"about":"${Disclosure.ABOUT_LINE}"}""", emptyList(), 1_700_000_000))
        agent.relays.acceptEvents = false
        val outcome = agent.call("talk_publish_post", "markdown" to text("one"))
        assertFalse(outcome.json.getValue("published").jsonPrimitive.boolean)
        assertTrue(agent.call("talk_publish_post", "markdown" to text("one")).isError)
    }

    @Test
    fun contentRulesApply() = runTest {
        val agent = Agent()
        assertTrue(agent.call("talk_publish_post", "markdown" to text("x".repeat(10_001))).isError)
        assertTrue(agent.call("talk_publish_post", "markdown" to text("   ")).isError)
        assertTrue(agent.call("talk_publish_post", "markdown" to text("spoof​text")).isError)
        assertTrue(agent.relays.published.isEmpty())
    }

    @Test
    fun aReplyTagsItsThreadAndHasTheShorterLimit() = runTest {
        val agent = Agent()
        val root = agent.relays.seedPost("Root")
        val parent = agent.relays.seed(other, TalkEvents.reply("Parent", emptyList(), com.yet.talk.api.schema.PostRef(root.id, root.pubkey), null, 1_700_000_100))
        assertTrue(agent.call("talk_reply", "post_id" to text(parent.id), "markdown" to text("x".repeat(5_001))).isError)
        assertFalse(agent.call("talk_reply", "post_id" to text(parent.id), "markdown" to text("My reply")).isError)
        val reply = agent.events(Kinds.TEXT_NOTE).single()
        assertTrue(reply.has("l", "reply", "com.yet.talk"))
        assertEquals("root", reply.tags.first { it[0] == "e" && it[1] == root.id }[3])
        assertEquals("reply", reply.tags.first { it[0] == "e" && it[1] == parent.id }[3])
        assertTrue(reply.has("p", other.publicKey))
    }

    @Test
    fun aQuotePointsAtThePost() = runTest {
        val agent = Agent()
        val post = agent.relays.seedPost()
        agent.call("talk_quote", "post_id" to text(post.id), "markdown" to text("Worth reading"))
        val quote = agent.events(Kinds.TEXT_NOTE).single()
        assertTrue(quote.tags.any { it[0] == "q" && it[1] == post.id })
        assertTrue(quote.has("l", "post", "com.yet.talk"))
    }

    @Test
    fun onlyTalkPostsCanBeRepliedTo() = runTest {
        val agent = Agent()
        val outside = agent.relays.seed(other, EventTemplate(Kinds.TEXT_NOTE, "Not Talk", emptyList(), 1_700_000_000))
        assertTrue(agent.call("talk_reply", "post_id" to text(outside.id), "markdown" to text("Hi")).isError)
    }

    @Test
    fun aLikeIsLabelledAndNotRepeated() = runTest {
        val agent = Agent()
        val post = agent.relays.seedPost()
        agent.call("talk_like", "post_id" to text(post.id))
        val like = agent.events(Kinds.REACTION).single()
        assertEquals("+", like.content)
        assertTrue(like.has("l", "reaction", "com.yet.talk"))
        agent.advance(10)
        assertFalse(agent.call("talk_like", "post_id" to text(post.id)).isError)
        assertEquals(1, agent.events(Kinds.REACTION).size)
    }

    @Test
    fun aFollowKeepsTheListAndIsNewerThanIt() = runTest {
        val agent = Agent()
        val friend = SecretKey.generate().publicKey
        val future = agent.clock.seconds + 50
        agent.relays.seed(agent.key, EventTemplate(Kinds.METADATA, """{"name":"x","bot":true,"about":"${Disclosure.ABOUT_LINE}"}""", emptyList(), 1_700_000_000))
        agent.relays.seed(agent.key, contactList(listOf(friend), future))
        agent.call("talk_follow", "pubkey" to text(other.publicKey))
        val list = agent.events(Kinds.CONTACTS).single()
        assertTrue(list.has("p", friend))
        assertTrue(list.has("p", other.publicKey))
        assertEquals(future + 1, list.createdAt)
    }

    @Test
    fun aFollowListThatWasThereIsNotReplacedWhenRelaysReturnNone() = runTest {
        val agent = Agent()
        agent.call("talk_follow", "pubkey" to text(other.publicKey))
        agent.relays.stored.removeAll { it.kind == Kinds.CONTACTS }
        agent.advance(10)
        assertTrue(agent.call("talk_follow", "pubkey" to text(SecretKey.generate().publicKey)).isError)
    }
}
