package com.yet.talk.mcp

import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.api.source.chat.ChatRemoteSource
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.Nip59
import com.yet.talk.nostr.signer.LocalKeySigner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ChatsTest {
    private val relays = FakeRelays()
    private val clock = TestClock()
    private val alice = Agent(relays = relays, clock = clock)
    private val bob = Agent(relays = relays, clock = clock)

    private fun recipients(vararg pubkeys: String) = "recipients" to JsonArray(pubkeys.map(::JsonPrimitive))

    private fun text(value: String) = "text" to JsonPrimitive(value)

    private suspend fun giveInbox(agent: Agent, url: String = "wss://inbox-${agent.pubkey.take(4)}.test") =
        relays.seed(agent.key, TalkEvents.inboxRelays(listOf(url), clock.seconds))

    private fun wraps(): List<Pair<Event, Set<String>>> = relays.published.filter { it.first.kind == Kinds.GIFT_WRAP }

    private fun JsonObject.messages() = getValue("messages").jsonArray.map { it.jsonObject }

    @Test
    fun twoAgentsCanTalk() = runTest {
        giveInbox(alice)
        giveInbox(bob)
        assertFalse(alice.call("talk_send_chat", recipients(bob.pubkey), text("Hello Bob")).isError)
        val received = bob.call("talk_read_chats").json.messages().single()
        assertEquals("Hello Bob", received.getValue("text").jsonPrimitive.content)
        assertEquals(alice.pubkey, received.getValue("from").jsonPrimitive.content)
        assertEquals(listOf(JsonPrimitive(alice.pubkey)), received.getValue("chat_with").jsonArray.toList())
    }

    @Test
    fun theSenderKeepsACopy() = runTest {
        giveInbox(alice)
        giveInbox(bob)
        alice.call("talk_send_chat", recipients(bob.pubkey), text("Note to self too"))
        val copy = alice.call("talk_read_chats").json.messages().single()
        assertEquals("true", copy.getValue("from_me").jsonPrimitive.content)
    }

    @Test
    fun oneWrapPerMemberGoesToTheirInboxAndRevealsNothing() = runTest {
        val bobInbox = giveInbox(bob).tags.first { it[0] == "relay" }[1]
        giveInbox(alice)
        alice.call("talk_send_chat", recipients(bob.pubkey), text("A secret message"))
        assertEquals(2, wraps().size)
        val toBob = wraps().single { it.first.tags.any { tag -> tag == listOf("p", bob.pubkey) } }
        assertEquals(setOf(bobInbox), toBob.second)
        assertFalse("A secret message" in toBob.first.content)
        assertTrue(toBob.first.pubkey != alice.pubkey, "the wrap is signed by a throwaway key")
        val rumor = assertNotNull(Nip59.unwrap(toBob.first, LocalKeySigner(bob.key)))
        assertEquals(Kinds.CHAT_MESSAGE, rumor.kind)
        assertEquals(listOf(listOf("p", bob.pubkey)), rumor.tags)
        assertEquals(clock.seconds, rumor.createdAt)
    }

    @Test
    fun someoneWithoutInboxRelaysCannotBeWritten() = runTest {
        giveInbox(alice)
        val outcome = alice.call("talk_send_chat", recipients(bob.pubkey), text("Hello"))
        assertTrue(outcome.isError)
        assertTrue(wraps().isEmpty())
    }

    @Test
    fun anAgentWithoutInboxRelaysPublishesThemFirst() = runTest {
        giveInbox(bob)
        alice.call("talk_send_chat", recipients(bob.pubkey), text("Hello"))
        val list = relays.published.map { it.first }.single { it.kind == Kinds.DM_RELAYS }
        assertEquals(ChatRemoteSource.DEFAULT_INBOX, list.tags.map { it[1] })
    }

    @Test
    fun aChatDryRunSendsNothing() = runTest {
        val dry = Agent(relays = relays, clock = clock, publishEnabled = false)
        giveInbox(bob)
        val outcome = dry.call("talk_send_chat", recipients(bob.pubkey), text("Hello"))
        assertFalse(outcome.isError, outcome.text)
        assertEquals("true", outcome.json.getValue("dry_run").jsonPrimitive.content)
        assertTrue(relays.published.isEmpty())
    }

    @Test
    fun chatsAreRateLimited() = runTest {
        giveInbox(alice)
        giveInbox(bob)
        assertFalse(alice.call("talk_send_chat", recipients(bob.pubkey), text("One")).isError)
        assertTrue(alice.call("talk_send_chat", recipients(bob.pubkey), text("Two")).isError)
    }

    @Test
    fun aGroupIsLimitedAndNamed() = runTest {
        giveInbox(alice)
        giveInbox(bob)
        val carol = Agent(relays = relays, clock = clock)
        giveInbox(carol)
        val many = Array(11) { SecretKey.generate().publicKey }
        assertTrue(alice.call("talk_send_chat", recipients(*many), text("Hi")).isError)
        alice.call("talk_send_chat", recipients(bob.pubkey, carol.pubkey), text("Hi all"), "subject" to JsonPrimitive("Papers"))
        assertEquals(3, wraps().size)
        val received = carol.call("talk_read_chats", "with" to JsonArray(listOf(JsonPrimitive(alice.pubkey), JsonPrimitive(bob.pubkey)))).json.messages().single()
        assertEquals("Papers", received.getValue("subject").jsonPrimitive.content)
    }

    @Test
    fun readingCanNarrowToOneChat() = runTest {
        giveInbox(alice)
        giveInbox(bob)
        val carol = Agent(relays = relays, clock = clock)
        giveInbox(carol)
        alice.call("talk_send_chat", recipients(carol.pubkey), text("From Alice"))
        bob.call("talk_send_chat", recipients(carol.pubkey), text("From Bob"))
        val onlyBob = carol.call("talk_read_chats", "with" to JsonArray(listOf(JsonPrimitive(bob.pubkey)))).json.messages()
        assertEquals(listOf("From Bob"), onlyBob.map { it.getValue("text").jsonPrimitive.content })
    }

    @Test
    fun someoneElsesWrapIsNotReadable() = runTest {
        giveInbox(alice)
        giveInbox(bob)
        val eve = Agent(relays = relays, clock = clock)
        giveInbox(eve)
        alice.call("talk_send_chat", recipients(bob.pubkey), text("Private"))
        assertTrue(eve.call("talk_read_chats").json.messages().isEmpty())
    }

    @Test
    fun aMessageDeletedByItsAuthorIsLeftOut() = runTest {
        giveInbox(alice)
        giveInbox(bob)
        alice.call("talk_send_chat", recipients(bob.pubkey), text("Oops"))
        val message = bob.call("talk_read_chats").json.messages().single().getValue("id").jsonPrimitive.content
        val deletion = com.yet.talk.nostr.nip.Rumor.of(alice.pubkey, TalkEvents.chatDeletion(listOf(bob.pubkey), listOf(message), clock.seconds))
        relays.stored += Nip59.wrap(deletion, LocalKeySigner(alice.key), bob.pubkey, clock.seconds)
        assertTrue(bob.call("talk_read_chats").json.messages().isEmpty())
    }
}
