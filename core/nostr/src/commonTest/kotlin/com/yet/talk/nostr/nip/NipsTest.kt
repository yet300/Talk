package com.yet.talk.nostr.nip

import com.yet.talk.nostr.event.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NipsTest {

    private val target = Event(
        id = "e".repeat(64), pubkey = "p".repeat(64), createdAt = 1, kind = 1,
        tags = emptyList(), content = "hi", sig = "s".repeat(128),
    )

    @Test
    fun parsesMarkedThreadTags() {
        val reference = ThreadReference.parse(
            listOf(listOf("e", "root", "", "root"), listOf("e", "m", "", "mention"), listOf("e", "parent", "wss://r", "reply")),
        )
        assertEquals(ThreadReference(root = "root", reply = "parent", mentions = listOf("m")), reference)
        assertEquals("parent", reference.parent)
    }

    @Test
    fun parsesDirectReplyAndPositionalTags() {
        assertEquals("root", ThreadReference.parse(listOf(listOf("e", "root", "", "root"))).parent)
        val positional = ThreadReference.parse(listOf(listOf("e", "a"), listOf("e", "b"), listOf("e", "c")))
        assertEquals(ThreadReference(root = "a", reply = "c", mentions = listOf("b")), positional)
        assertFalse(ThreadReference.parse(listOf(listOf("p", "x"))).isReply)
    }

    @Test
    fun buildsReactionsRepostsAndDeletions() {
        val like = reaction(target, createdAt = 5)
        assertEquals(Kinds.REACTION, like.kind)
        assertEquals("+", like.content)
        assertEquals(listOf(listOf("e", target.id, "", "", target.pubkey), listOf("p", target.pubkey), listOf("k", "1")), like.tags)

        val repost = repost(target, createdAt = 5)
        assertEquals(Kinds.REPOST, repost.kind)
        assertTrue(repost.content.contains(target.id))

        val delete = deletion(listOf(target), createdAt = 5)
        assertEquals(listOf(listOf("e", target.id), listOf("k", "1")), delete.tags)
    }

    @Test
    fun roundTripsListsAndImeta() {
        val relays = listOf(RelayListEntry("wss://a", true, true), RelayListEntry("wss://b", true, false), RelayListEntry("wss://c", false, true))
        val relayEvent = target.copy(kind = Kinds.RELAY_LIST, tags = relayList(relays, 1).tags)
        assertEquals(relays, relayEvent.relayListEntries())

        val contacts = target.copy(kind = Kinds.CONTACTS, tags = contactList(listOf("a", "b", "a"), 1).tags)
        assertEquals(listOf("a", "b"), contacts.contactPubkeys())

        val image = Imeta(url = "https://x/a.png", mimeType = "image/png", sha256 = "ff", width = 400, height = 300)
        assertEquals(image, Imeta.parse(image.toTag()))
        assertEquals(400, Imeta.parse(listOf("imeta", "url u", "dim 400.0x300.0"))?.width)
    }

    @Test
    fun parsesMetadataIncludingLegacyDisplayName() {
        assertEquals("Andrej", Metadata.parse(target.copy(content = """{"name":"karpathy","display_name":"Andrej"}"""))?.displayName)
        assertEquals("Old", Metadata.parse(target.copy(content = """{"displayName":"Old","x":1}"""))?.displayName)
        assertEquals(null, Metadata.parse(target.copy(content = "not json")))
    }
}
