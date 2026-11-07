package com.yet.talk.data

import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.data.fake.TestData
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.Disappearing
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.nostr.encoding.Nip19
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.Nip59
import com.yet.talk.nostr.nip.Rumor
import com.yet.talk.nostr.nip.firstValue
import com.yet.talk.nostr.nip.values
import com.yet.talk.nostr.signer.LocalKeySigner
import com.yet.talk.nostr.signer.Signer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Chats in full (design pass 4, step 3): replies, reactions, deletions, images, disappearing messages, groups, blocks. */
class ChatDataTest {

    private val bob = LocalKeySigner(SecretKey.generate())
    private val carol = LocalKeySigner(SecretKey.generate())
    private val myKey = SecretKey.generate()
    private val me = UserId(myKey.publicKey)

    private fun chatTest(body: suspend TestScope.(TestData) -> Unit) = runTest {
        val factory = testDriverFactory() ?: return@runTest
        body(TestData(factory, backgroundScope, StandardTestDispatcher(testScheduler)))
    }

    private suspend fun TestData.inbox(vararg who: Signer) = who.forEach { network.events += it.sign(TalkEvents.inboxRelays(listOf("wss://inbox"), 1)) }

    /** What [from] sends us, wrapped as it would arrive. */
    private suspend fun TestData.incoming(from: Signer, at: Long, template: (String) -> com.yet.talk.nostr.event.EventTemplate): Rumor {
        val rumor = Rumor.of(from.publicKey(), template(me.value))
        network.events += Nip59.wrap(rumor, from, me.value, now = at)
        return rumor
    }

    private suspend fun TestData.signIn() = accounts.importKey(Nip19.nsec(myKey)).getOrThrow()

    /** The rumors wrapped for [who] that we published, newest last. */
    private suspend fun TestData.sentTo(who: Signer): List<Rumor> {
        val pubkey = who.publicKey()
        return network.published.filter { it.kind == Kinds.GIFT_WRAP && listOf("p", pubkey) in it.tags }.mapNotNull { Nip59.unwrap(it, who) }
    }

    @Test
    fun repliesReactionsAndDeletionsTravelInsideTheChat() = chatTest { data ->
        data.inbox(bob)
        val hi = data.incoming(bob, 1_000) { TalkEvents.chatMessage(listOf(it), "hi", 1_000) }
        val bobKey = bob.publicKey()
        val oops = data.incoming(bob, 1_001) { TalkEvents.chatMessage(listOf(it), "oops", 1_001) }
        data.incoming(bob, 1_002) { TalkEvents.chatReaction(listOf(it), hi.id, bobKey, "👍", Kinds.CHAT_MESSAGE, 1_002) }
        data.incoming(bob, 1_003) { TalkEvents.chatDeletion(listOf(it), listOf(oops.id), 1_003) }
        data.signIn()
        val chat = data.chats.chats().first { it.isNotEmpty() }.single().id
        // Bob deleted his second message for everyone; his own reaction sits under the first.
        val first = data.chats.messages(chat).first { messages -> messages.singleOrNull()?.reactions?.isNotEmpty() == true }.single()
        assertEquals("hi", first.content)
        assertEquals(listOf("👍" to false), first.reactions.map { it.emoji to it.isMine })

        data.chats.send(chat, "yes", replyTo = hi.id).getOrThrow()
        val reply = data.chats.messages(chat).first().last()
        assertEquals("hi", reply.replyTo?.text)
        assertEquals(hi.id, data.sentTo(bob).last().tags.values("e").single())

        data.chats.react(hi.id, "❤️").getOrThrow()
        assertEquals(setOf("👍" to false, "❤️" to true), data.chats.messages(chat).first().first().reactions.map { it.emoji to it.isMine }.toSet())
        val reaction = data.sentTo(bob).last()
        assertEquals(Kinds.REACTION to "❤️", reaction.kind to reaction.content)

        data.chats.deleteForEveryone(reply.id).getOrThrow()
        assertEquals(listOf("hi"), data.chats.messages(chat).first().map { it.content })
        val deletion = data.sentTo(bob).last()
        assertEquals(Kinds.DELETION to listOf(reply.id), deletion.kind to deletion.tags.values("e"))

        data.chats.deleteForMe(hi.id)
        assertTrue(data.chats.messages(chat).first().isEmpty())
        // Bob cannot delete messages that are not his.
        assertEquals(DomainError.NotFound, data.chats.deleteForEveryone(hi.id).exceptionOrNull())
    }

    @Test
    fun imagesAreEncryptedBeforeTheyLeaveTheDevice() = chatTest { data ->
        data.inbox(bob)
        data.signIn()
        val chat = ChatId.of(listOf(UserId(bob.publicKey())))
        val picture = ByteArray(2_000) { it.toByte() }
        data.chats.sendImage(chat, ImageFile(picture, "image/png")).getOrThrow()

        val uploaded = data.network.blobs.values.single()
        assertFalse(uploaded.contentEquals(picture), "Blossom only sees ciphertext")
        val message = data.chats.messages(chat).first().single()
        assertNotNull(message.image)
        assertContentEquals(picture, data.chats.image(message.id).getOrThrow())

        val file = data.sentTo(bob).single()
        assertEquals(Kinds.CHAT_FILE to "aes-gcm", file.kind to file.tags.firstValue("encryption-algorithm"))
        assertEquals("image/png", file.tags.firstValue("file-type"))
    }

    @Test
    fun disappearingMessagesExpireEverywhere() = chatTest { data ->
        data.inbox(bob)
        val now = data.clock.seconds
        data.incoming(bob, now) { TalkEvents.chatMessage(listOf(it), "already gone", now - 10, expiresAt = now - 1) }
        data.signIn()
        val chat = ChatId.of(listOf(UserId(bob.publicKey())))
        data.chats.setDisappearing(chat, Disappearing.Hour)
        assertEquals(Disappearing.Hour.duration, data.chats.chat(chat).first()?.disappearing)

        data.chats.send(chat, "soon gone").getOrThrow()
        val message = data.chats.messages(chat).first().single()
        assertEquals(now + 3_600, message.expiresAt?.epochSeconds)
        // The wrap asks relays to drop it at the same time.
        val wrap = data.network.published.last { it.kind == Kinds.GIFT_WRAP }
        assertEquals((now + 3_600).toString(), wrap.tags.firstValue("expiration"))

        data.clock.seconds += 3_601
        val expired = data.chats.messages(chat).first().single()
        assertTrue(expired.isExpired)
        assertEquals("", expired.content)
    }

    @Test
    fun groupsCarryTheirNameToEveryMember() = chatTest { data ->
        data.inbox(bob, carol)
        data.signIn()
        val group = ChatId.of(listOf(UserId(bob.publicKey()), UserId(carol.publicKey())))
        data.chats.createGroup(group, "Storage nerds").getOrThrow()
        data.chats.send(group, "hello both").getOrThrow()

        assertEquals("Storage nerds", data.chats.chats().first().single().name)
        listOf(bob, carol).forEach { member ->
            val rumor = data.sentTo(member).single()
            assertEquals("Storage nerds", rumor.tags.firstValue("subject"))
            assertEquals(setOf(bob.publicKey(), carol.publicKey()), rumor.tags.values("p").toSet())
        }
        val tooMany = ChatId.of((0..10).map { UserId(SecretKey.generate().publicKey) })
        assertEquals(DomainError.TooManyMembers(10), data.chats.createGroup(tooMany, null).exceptionOrNull())
    }

    @Test
    fun blockingIsPrivateAndHidesTheirChats() = chatTest { data ->
        data.inbox(bob)
        data.incoming(bob, 1_000) { TalkEvents.chatMessage(listOf(it), "buy my coin", 1_000) }
        data.signIn()
        data.chats.chats().first { it.isNotEmpty() }

        data.moderation.block(UserId(bob.publicKey())).getOrThrow()
        assertTrue(data.chats.chats().first().isEmpty())
        assertTrue(UserId(bob.publicKey()) in data.moderation.moderation.first().blocked)
        val list: Event = data.network.published.last { it.kind == Kinds.MUTE_LIST }
        assertTrue(list.tags.none { it.firstOrNull() == "p" }, "nobody can see whom you blocked")
        val me = LocalKeySigner(myKey)
        assertTrue(bob.publicKey() in me.nip44Decrypt(list.content, me.publicKey()))

        data.clock.seconds += 1
        data.moderation.unblock(UserId(bob.publicKey())).getOrThrow()
        assertEquals(1, data.chats.chats().first().size)
    }
}
