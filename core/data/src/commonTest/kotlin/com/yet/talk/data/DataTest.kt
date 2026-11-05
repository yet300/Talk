package com.yet.talk.data

import androidx.paging.testing.asSnapshot
import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.data.fake.TestData
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.Feed
import com.yet.talk.nostr.encoding.Nip19
import com.yet.talk.nostr.event.EventTemplate
import com.yet.talk.nostr.key.SecretKey
import com.yet.talk.nostr.nip.Kinds
import com.yet.talk.nostr.nip.contactPubkeys
import com.yet.talk.nostr.signer.LocalKeySigner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class DataTest {

    private val bob = LocalKeySigner(SecretKey.generate())

    private fun dataTest(body: suspend TestScope.(TestData) -> Unit) = runTest {
        val factory = testDriverFactory() ?: return@runTest
        // Database work on the test scheduler, so Paging's idle detection (asSnapshot) sees every load.
        body(TestData(factory, backgroundScope, StandardTestDispatcher(testScheduler)))
    }

    private suspend fun TestData.bobPost(content: String, createdAt: Long) =
        bob.sign(TalkEvents.post(content, emptyList(), null, createdAt)).also { network.events += it }

    @Test
    fun accountLifecycle() = dataTest { data ->
        assertNull(data.accounts.account.first())
        assertEquals(DomainError.InvalidKey, data.accounts.importKey("npub1nope").exceptionOrNull())

        val key = SecretKey.generate()
        val account = data.accounts.importKey(Nip19.nsec(key)).getOrThrow()
        assertEquals(key.publicKey, account.userId.value)
        assertEquals(account, data.accounts.account.first())
        assertEquals(key.toHex(), data.vault.stored, "the key is persisted in the vault")
        assertEquals(Nip19.nsec(key), data.accounts.exportSecretKey().getOrThrow())

        data.accounts.signOut()
        assertNull(data.accounts.account.first())
        assertNull(data.vault.stored)
    }

    @Test
    fun publishingIsOptimisticAndReportsRejection() = dataTest { data ->
        data.accounts.createAccount()

        val id = data.posts.publish("hello", emptyList(), null).getOrThrow()
        assertEquals(PublishState.Published, data.posts.observePost(id).first()?.publishState)

        data.network.accept = false
        data.clock.seconds += 1
        assertEquals(DomainError.NoRelayAccepted, data.posts.publish("again", emptyList(), null).exceptionOrNull())
        val failed = data.posts.feed(Feed.Latest).asSnapshot().first()
        assertEquals("again" to PublishState.Failed, failed.content to failed.publishState)
    }

    @Test
    fun writesNeedAnAccount() = dataTest { data ->
        assertEquals(DomainError.NotSignedIn, data.posts.publish("x", emptyList(), null).exceptionOrNull())
    }

    @Test
    fun latestFeedPagesOlderPostsUntilTheEnd() = dataTest { data ->
        repeat(25) { data.bobPost("post $it", createdAt = 1_000L + it) }
        val all = data.posts.feed(Feed.Latest).asSnapshot { appendScrollWhile { true } }
        assertEquals((24 downTo 0).map { "post $it" }, all.map { it.content })
        // Refresh (20 newest), then pages that start one second before the oldest event seen, until relays have nothing older.
        assertEquals(listOf(null, 1_004L, 999L), data.network.timelineUntils)
    }

    @Test
    fun refreshAfterAPauseFillsTheGap() = dataTest { data ->
        repeat(25) { data.bobPost("old $it", createdAt = 1_000L + it) }
        val feed = data.posts.feed(Feed.Latest)
        feed.asSnapshot { appendScrollWhile { true } }

        repeat(60) { data.bobPost("new $it", createdAt = 2_000L + it) } // the app was closed meanwhile
        // The old pages already reached the end of the feed: only the refresh can fill the gap.
        val all = feed.asSnapshot { appendScrollWhile { true } }

        assertEquals((59 downTo 0).map { "new $it" } + (24 downTo 0).map { "old $it" }, all.map { it.content })
    }

    @Test
    fun likeAndUnlikeUpdateTheViewerAndPublishADeletion() = dataTest { data ->
        data.accounts.createAccount()
        val post = data.bobPost("like me", 1_000)
        data.posts.refreshPost(PostId(post.id)).getOrThrow()

        data.reactions.setLiked(PostId(post.id), liked = true).getOrThrow()
        val liked = data.posts.observePost(PostId(post.id)).first()!!
        assertTrue(liked.viewer.liked)
        assertEquals(1, liked.stats.likes)

        data.reactions.setLiked(PostId(post.id), liked = false).getOrThrow()
        assertEquals(false, data.posts.observePost(PostId(post.id)).first()!!.viewer.liked)
        assertEquals(Kinds.DELETION, data.network.published.last().kind)
    }

    @Test
    fun failedLikeIsRolledBack() = dataTest { data ->
        data.accounts.createAccount()
        val post = data.bobPost("x", 1_000)
        data.posts.refreshPost(PostId(post.id)).getOrThrow()
        data.network.accept = false

        assertTrue(data.reactions.setLiked(PostId(post.id), true).isFailure)
        assertEquals(0, data.posts.observePost(PostId(post.id)).first()!!.stats.likes)
    }

    @Test
    fun editingCreatesTheNextRevisionOnlyForTheAuthor() = dataTest { data ->
        data.accounts.createAccount()
        val id = data.posts.publish("v1", emptyList(), null).getOrThrow()
        data.clock.seconds += 10

        data.posts.edit(id, "v2", emptyList()).getOrThrow()
        val edited = data.posts.observePost(id).first()!!
        assertEquals("v2" to 2, edited.content to edited.revision)
        assertEquals(listOf(2, 1), data.posts.observeRevisions(id).first().map { it.number })

        val bobs = data.bobPost("not yours", 1_000)
        data.posts.refreshPost(PostId(bobs.id))
        assertEquals(DomainError.NotFound, data.posts.edit(PostId(bobs.id), "hacked", emptyList()).exceptionOrNull())
    }

    @Test
    fun revisionsAndDeletionsFromOthersAreIgnored() = dataTest { data ->
        val mallory = LocalKeySigner(SecretKey.generate())
        val post = data.bobPost("original", 1_000)
        data.network.events += mallory.sign(TalkEvents.revision(post.id, 2, "forged", emptyList(), 1_001))
        data.network.events += mallory.sign(TalkEvents.delete(mapOf(post.id to Kinds.TEXT_NOTE), 1_002))

        data.posts.refreshPost(PostId(post.id)).getOrThrow()

        val stored = data.posts.observePost(PostId(post.id)).first()!!
        assertEquals("original" to 1, stored.content to stored.revision)
    }

    @Test
    fun trendingRanksByEngagementWithoutSelfEngagement() = dataTest { data ->
        val now = data.clock.seconds
        val quiet = data.bobPost("quiet", now - 3_600)
        val popular = data.bobPost("popular", now - 3_600)
        repeat(3) {
            val fan = LocalKeySigner(SecretKey.generate())
            data.network.events += fan.sign(TalkEvents.like(popular, now - 60))
        }
        data.network.events += bob.sign(TalkEvents.like(quiet, now - 60)) // self-like, not counted

        assertEquals(listOf("popular"), data.trending.trending().asSnapshot().map { it.content })
    }

    @Test
    fun followingKeepsTheExistingListAndFeedsTheFollowingTimeline() = dataTest { data ->
        val me = data.accounts.createAccount().getOrThrow().userId
        val carol = LocalKeySigner(SecretKey.generate())
        data.network.events += LocalKeySigner(SecretKey.fromHex(data.vault.stored!!)!!)
            .sign(EventTemplate(kind = Kinds.CONTACTS, content = "", tags = listOf(listOf("p", carol.publicKey())), createdAt = data.clock.seconds))

        data.follows.setFollowing(UserId(bob.publicKey()), true).getOrThrow()

        val contacts = data.network.published.last { it.kind == Kinds.CONTACTS }
        assertEquals(setOf(carol.publicKey(), bob.publicKey()), contacts.contactPubkeys().toSet())
        assertTrue(contacts.createdAt > data.clock.seconds - 1)
        assertTrue(data.follows.observeIsFollowing(UserId(bob.publicKey())).first())

        data.bobPost("for followers", 1_000)
        assertEquals(listOf("for followers"), data.posts.feed(Feed.Following).asSnapshot().map { it.content })
        assertTrue(data.syncDao.state("following:${me.value}") != null)

        data.follows.setFollowing(UserId(carol.publicKey()), false).getOrThrow()
        assertNull(data.syncDao.state("following:${me.value}"), "a new contact list restarts Following's remote paging")
        assertTrue(me.value.isNotEmpty())
    }

    @Test
    fun bookmarksRoundTrip() = dataTest { data ->
        data.accounts.createAccount()
        val post = data.bobPost("save me", 1_000)
        data.posts.refreshPost(PostId(post.id)).getOrThrow()

        data.bookmarks.setBookmarked(PostId(post.id), true).getOrThrow()
        assertEquals(listOf("save me"), data.bookmarks.bookmarks().asSnapshot().map { it.content })
        assertTrue(data.posts.observePost(PostId(post.id)).first()!!.viewer.bookmarked)

        data.clock.seconds += 1
        data.bookmarks.setBookmarked(PostId(post.id), false).getOrThrow()
        assertTrue(data.bookmarks.bookmarks().asSnapshot().isEmpty())
    }
}
