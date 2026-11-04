package com.yet.talk.api

import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.api.schema.TalkSchema
import com.yet.talk.api.source.media.BlossomClient
import com.yet.talk.api.source.nip05.Nip05Client
import com.yet.talk.api.source.post.RelayPostRemoteSource
import com.yet.talk.api.source.publish.RelayPublishRemoteSource
import com.yet.talk.api.source.social.RelaySocialRemoteSource
import com.yet.talk.nostr.event.Event
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.event.EventVerifier
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.nip.contactList
import com.yet.talk.nostr.signer.LocalKeySigner
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.kotlincrypto.hash.sha2.SHA256

class RemoteSourceTest {

    private val alice = LocalKeySigner(SecretKey.generate())
    private val bob = LocalKeySigner(SecretKey.generate())

    @Test
    fun timelineAsksForLabelledNotesAndDropsForeignOnes() = runTest {
        val talk = alice.sign(TalkEvents.post("talk", emptyList(), null, 2))
        val foreign = alice.sign(EventTemplate(kind = 1, content = "foreign", createdAt = 3))
        val client = FakeRelayClient(listOf(foreign, talk))

        val posts = RelayPostRemoteSource(client, TestRelays).timeline(authors = null, until = 10, limit = 20)

        assertEquals(listOf(talk), posts.map { it.event })
        val filter = client.queries.single().single()
        assertEquals(listOf(1), filter.kinds)
        assertEquals(mapOf('L' to listOf(TalkSchema.NAMESPACE)), filter.tags)
        assertEquals(10, filter.until)
    }

    @Test
    fun timelineWithNoAuthorsDoesNotQuery() = runTest {
        val client = FakeRelayClient()
        assertTrue(RelayPostRemoteSource(client, TestRelays).timeline(authors = emptyList(), until = null, limit = 20).isEmpty())
        assertTrue(client.queries.isEmpty())
    }

    @Test
    fun relatedChunksIdsAndAsksForAttachments() = runTest {
        val client = FakeRelayClient()
        RelayPostRemoteSource(client, TestRelays).related(List(150) { "id$it" })

        val filters = client.queries.single()
        assertEquals(4, filters.size, "two chunks of ids, two filters each")
        assertEquals(100, filters.first().tags.getValue('e').size)
    }

    @Test
    fun followersAreContactListsThatReallyIncludeTheUser() = runTest {
        val me = alice.publicKey()
        val follower = bob.sign(contactList(listOf(me), createdAt = 1))
        val lying = bob.sign(contactList(listOf("someone-else"), createdAt = 2))
        val client = FakeRelayClient(listOf(follower, lying))

        assertEquals(listOf(follower), RelaySocialRemoteSource(client, TestRelays).followers(me, 50).map { it.event })
    }

    @Test
    fun listsKeepTheNewestVersionPerKind() = runTest {
        val old = alice.sign(contactList(listOf("a"), createdAt = 1))
        val new = alice.sign(contactList(listOf("b"), createdAt = 2))
        val lists = RelaySocialRemoteSource(FakeRelayClient(listOf(old, new)), TestRelays).lists(alice.publicKey())
        assertEquals(listOf(new), lists.map { it.event })
    }

    @Test
    fun publishGoesToWriteRelays() = runTest {
        val client = FakeRelayClient()
        val event = alice.sign(TalkEvents.post("x", emptyList(), null, 1))
        RelayPublishRemoteSource(client, TestRelays).publish(event)
        assertEquals(listOf("wss://write"), client.published.single().second)
    }

    @Test
    fun blossomUploadSendsASignedAuthorizationForTheBlobHash() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        val hash = SHA256().digest(bytes).toHexString()
        var authorization: Event? = null
        val engine = MockEngine { request ->
            val header = request.headers[HttpHeaders.Authorization]!!.removePrefix("Nostr ")
            authorization = Json.decodeFromString(Event.serializer(), Base64.decode(header).decodeToString())
            respond(
                """{"url":"https://blossom/$hash.png","sha256":"$hash","size":3,"type":"image/png","uploaded":1}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val blob = BlossomClient(HttpClient(engine)).upload(bytes, "image/png", alice, createdAt = 100)

        assertEquals(hash, blob.sha256)
        val auth = authorization!!
        assertTrue(EventVerifier.verify(auth))
        assertEquals(24242, auth.kind)
        assertTrue(listOf("x", hash) in auth.tags && listOf("t", "upload") in auth.tags)
    }

    @Test
    fun nip05MatchesOnlyTheRightKey() = runTest {
        val engine = MockEngine { respond("""{"names":{"bob":"ABCD"}}""", HttpStatusCode.OK) }
        val client = Nip05Client(HttpClient(engine))
        assertTrue(client.verify("Bob@example.com", "abcd"))
        assertFalse(client.verify("bob@example.com", "ffff"))
        assertFalse(client.verify("not-an-identifier", "abcd"))
    }
}
