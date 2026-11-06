package com.yet.talk.data

import androidx.paging.testing.asSnapshot
import com.yet.talk.api.schema.PostRef
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
import com.yet.talk.domain.model.user.ProfileEdit
import com.yet.talk.api.source.github.GistDto
import com.yet.talk.api.source.github.RepoDto
import com.yet.talk.domain.model.github.GitHubAccount
import com.yet.talk.domain.model.github.GitHubCard
import com.yet.talk.domain.model.github.GitHubRef
import com.yet.talk.nostr.nip.githubProofText
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        data.accounts.importKey(data.accounts.generateKey().getOrThrow())

        val id = data.posts.publish("hello", emptyList(), null).getOrThrow()
        assertEquals(PublishState.Published, data.posts.observePost(id).first()?.publishState)

        data.network.accept = false
        data.clock.seconds += 1
        assertEquals(DomainError.NoRelayAccepted, data.posts.publish("again", emptyList(), null).exceptionOrNull())
        val failed = data.posts.feed(Feed.Latest).asSnapshot().first()
        assertEquals("again" to PublishState.Failed, failed.content to failed.publishState)
    }

    @Test
    fun profileSaveKeepsFieldsOtherAppsSet() = dataTest { data ->
        val key = SecretKey.generate()
        val me = LocalKeySigner(key)
        data.accounts.importKey(Nip19.nsec(key)).getOrThrow()
        val previousAt = data.clock.seconds + 100 // a clock ahead of ours: the new version must still be newer
        data.network.events += me.sign(EventTemplate(Kinds.METADATA, """{"name":"old","lud16":"me@ln.example","about":"hi","picture":"https://nostr.build/a.gif"}""", createdAt = previousAt))

        data.profiles.updateMyProfile(ProfileEdit(name = "new", banner = "https://images.unsplash.com/photo-1")).getOrThrow()

        val saved = data.network.published.single()
        assertEquals(previousAt + 1, saved.createdAt)
        assertEquals(
            mapOf("name" to "new", "lud16" to "me@ln.example", "picture" to "https://nostr.build/a.gif", "banner" to "https://images.unsplash.com/photo-1"),
            Json.parseToJsonElement(saved.content).jsonObject.mapValues { it.value.jsonPrimitive.content },
        )
        val user = data.profiles.observeUser(UserId(me.publicKey())).first()
        // Kept on relays, but Talk shows avatars only from allowed sites.
        assertEquals(Triple("new", "https://images.unsplash.com/photo-1", null), Triple(user.name, user.banner, user.picture))
        assertTrue(data.profiles.verifyNip05(user.id, "me@example.com").getOrThrow())
    }

    @Test
    fun gitHubLinkIsVerifiedByItsGistAndSavedWithTheOtherTags() = dataTest { data ->
        val key = SecretKey.generate()
        val me = LocalKeySigner(key)
        data.accounts.importKey(Nip19.nsec(key)).getOrThrow()
        val otherTag = listOf("i", "mastodon:me@example.social", "123")
        data.network.events += me.sign(EventTemplate(Kinds.METADATA, """{"name":"me"}""", listOf(otherTag), createdAt = 1))
        val proof = data.github.proofText().getOrThrow()
        data.network.gists["abc123abc123abc123ab"] = GistDto("Me-Dev", "https://avatars.githubusercontent.com/u/1", "hello\n$proof")
        data.network.gists["ffff0000ffff0000ffff"] = GistDto("someone-else", null, proof)

        assertEquals(DomainError.GitHubProofMissing, data.github.verifyLink("me-dev", "https://gist.github.com/someone-else/ffff0000ffff0000ffff").exceptionOrNull())
        assertEquals(DomainError.InvalidGistLink, data.github.verifyLink("me-dev", "https://example.com/x").exceptionOrNull())
        val account = data.github.verifyLink("me-dev", "https://gist.github.com/Me-Dev/abc123abc123abc123ab").getOrThrow()
        assertEquals(GitHubAccount("Me-Dev", "abc123abc123abc123ab"), account)

        data.profiles.updateMyProfile(ProfileEdit(name = "me", github = account)).getOrThrow()
        assertEquals(listOf(otherTag, listOf("i", "github:Me-Dev", "abc123abc123abc123ab")), data.network.published.single().tags)
        assertEquals("Me-Dev", data.profiles.observeUser(UserId(me.publicKey())).first().github?.login)
    }

    @Test
    fun othersGitHubClaimsShowOnlyOnceTheGistProvesThem() = dataTest { data ->
        val bobId = UserId(bob.publicKey())
        data.network.events += bob.sign(EventTemplate(Kinds.METADATA, """{"name":"bob"}""", listOf(listOf("i", "github:bob", "0123456789abcdef0123")), createdAt = 1))
        data.profiles.refresh(bobId)
        assertNull(data.profiles.observeUser(bobId).first().github, "the gist does not exist")

        data.network.gists["0123456789abcdef0123"] = GistDto("bob", null, githubProofText(Nip19.npub(bob.publicKey())))
        data.profiles.refresh(bobId)
        assertNull(data.profiles.observeUser(bobId).first().github, "checked less than a day ago")
        data.clock.seconds += 24 * 60 * 60
        data.profiles.refresh(bobId)
        assertEquals("bob", data.profiles.observeUser(bobId).first().github?.login)
    }

    @Test
    fun gitHubCardsAreCached() = dataTest { data ->
        data.network.repos["ines/relaydiff"] = RepoDto("ines/relaydiff", "Diffs relays", "Rust", 248, 17, null)
        val ref = GitHubRef.Repo("ines", "relaydiff")
        val card = data.github.card(ref).toList().last() as GitHubCard.Repo
        assertEquals("ines/relaydiff" to 248L, card.fullName to card.stars)
        val calls = data.network.gitHubCalls
        assertEquals(card, data.github.card(ref).toList().last())
        assertEquals(calls, data.network.gitHubCalls, "served from the database")
    }

    @Test
    fun profileSaveRefusesToOverwriteAProfileItCannotRead() = dataTest { data ->
        val key = SecretKey.generate()
        val me = LocalKeySigner(key)
        data.accounts.importKey(Nip19.nsec(key)).getOrThrow()
        data.network.events += me.sign(EventTemplate(Kinds.METADATA, """{"name":"old"}""", createdAt = 1))
        data.profiles.refresh(UserId(me.publicKey())).getOrThrow() // cached
        data.network.events.clear() // relays now unreachable or empty

        assertTrue(data.profiles.updateMyProfile(ProfileEdit(name = "new")).exceptionOrNull() is DomainError.Network)
        assertTrue(data.network.published.isEmpty())
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
        data.accounts.importKey(data.accounts.generateKey().getOrThrow())
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
        data.accounts.importKey(data.accounts.generateKey().getOrThrow())
        val post = data.bobPost("x", 1_000)
        data.posts.refreshPost(PostId(post.id)).getOrThrow()
        data.network.accept = false

        assertTrue(data.reactions.setLiked(PostId(post.id), true).isFailure)
        assertEquals(0, data.posts.observePost(PostId(post.id)).first()!!.stats.likes)
    }

    @Test
    fun editingCreatesTheNextRevisionOnlyForTheAuthor() = dataTest { data ->
        data.accounts.importKey(data.accounts.generateKey().getOrThrow())
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
    fun followingShowsRepostsWithWhoRepostedAndRepliesSayWhoTheyAnswer() = dataTest { data ->
        data.accounts.importKey(data.accounts.generateKey().getOrThrow()).getOrThrow()
        data.follows.setFollowing(UserId(bob.publicKey()), true).getOrThrow()
        val carol = LocalKeySigner(SecretKey.generate())
        data.network.events += carol.sign(EventTemplate(Kinds.METADATA, """{"name":"carol"}""", createdAt = 1))
        val carolPost = carol.sign(TalkEvents.post("carol's post", emptyList(), null, 900)).also { data.network.events += it }
        data.network.events += bob.sign(TalkEvents.repost(carolPost, 1_100))
        data.bobPost("bob's own", 1_000)
        data.network.events += bob.sign(TalkEvents.reply("answer", emptyList(), PostRef(carolPost.id, carol.publicKey()), null, 1_050))

        val feed = data.posts.feed(Feed.Following).asSnapshot()
        assertEquals(listOf("carol's post", "bob's own"), feed.map { it.content }, "the repost (1100) comes first, once")
        assertEquals(bob.publicKey(), feed.first().repostedBy?.id?.value)
        assertNull(feed.last().repostedBy)

        val reply = data.posts.feed(Feed.UserReplies(UserId(bob.publicKey()))).asSnapshot().single()
        assertEquals("carol", reply.replyTo?.name)
    }

    @Test
    fun followingKeepsTheExistingListAndFeedsTheFollowingTimeline() = dataTest { data ->
        val me = data.accounts.importKey(data.accounts.generateKey().getOrThrow()).getOrThrow().userId
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
        data.accounts.importKey(data.accounts.generateKey().getOrThrow())
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
