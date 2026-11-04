package com.yet.talk.database

import com.app.common.AppDispatchers
import com.yet.talk.database.dao.sql.SqlMaintenanceDao
import com.yet.talk.database.dao.sql.SqlPostDao
import com.yet.talk.database.dao.sql.SqlProfileDao
import com.yet.talk.database.dao.sql.SqlReactionDao
import com.yet.talk.database.dao.sql.SqlActivityDao
import com.yet.talk.database.dao.sql.SqlBookmarkDao
import com.yet.talk.database.dao.sql.SqlFollowDao
import com.yet.talk.database.dao.sql.SqlRelayListDao
import com.yet.talk.database.dao.sql.SqlTrendingDao
import androidx.paging.PagingSource
import com.yet.talk.database.model.ActivityRecord
import com.yet.talk.database.model.PostRecord
import com.yet.talk.database.model.NewPostRecord
import com.yet.talk.database.model.PostFeed
import com.yet.talk.database.model.ProfileRecord
import com.yet.talk.database.model.PublishStates
import com.yet.talk.database.model.ReactionRecord
import com.yet.talk.database.model.ReactionTypes
import com.yet.talk.database.model.RelayRecord
import com.yet.talk.database.model.RevisionRecord
import com.yet.talk.database.model.TrendingRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class DaoTest {

    private class Daos(holder: DatabaseHolder, dispatchers: AppDispatchers) {
        val posts = SqlPostDao(holder, dispatchers)
        val profiles = SqlProfileDao(holder, dispatchers)
        val reactions = SqlReactionDao(holder, dispatchers)
        val follows = SqlFollowDao(holder, dispatchers)
        val bookmarks = SqlBookmarkDao(holder, dispatchers)
        val relayLists = SqlRelayListDao(holder, dispatchers)
        val activity = SqlActivityDao(holder, dispatchers)
        val trending = SqlTrendingDao(holder, dispatchers)
        val maintenance = SqlMaintenanceDao(holder, dispatchers)
    }

    /** One page, loaded the way Paging loads it. */
    private suspend fun Daos.feed(feed: PostFeed, viewer: String, limit: Int = 20): List<PostRecord> =
        posts.feedPages(feed, viewer)().firstPage(limit)

    private suspend fun Daos.activity(me: String): List<ActivityRecord> = activity.pages(me)().firstPage(20)

    private suspend fun <T : Any> PagingSource<Int, T>.firstPage(limit: Int): List<T> =
        assertIs<PagingSource.LoadResult.Page<Int, T>>(load(PagingSource.LoadParams.Refresh(null, limit, false))).data

    /** Skips on targets without a test driver. */
    private fun dbTest(body: suspend TestScope.(Daos) -> Unit) = runTest {
        val factory = testDriverFactory() ?: return@runTest
        body(Daos(DatabaseHolder(factory), AppDispatchers(io = Dispatchers.Default)))
    }

    private fun post(id: String, author: String = ALICE, createdAt: Long = 1, parent: String? = null, quote: String? = null) =
        NewPostRecord(id, author, rootId = parent, parentId = parent, quoteId = quote, createdAt = createdAt, content = "content $id",
            imagesJson = "[]", publishState = PublishStates.PUBLISHED, eventJson = "{}")

    private fun reaction(id: String, post: String, user: String, type: String = ReactionTypes.LIKE, createdAt: Long = 5) =
        ReactionRecord(id, post, user, type, createdAt)

    @Test
    fun latestFeedShowsTopLevelPostsNewestFirstWithAuthors() = dbTest { dao ->
        dao.profiles.upsert(ProfileRecord(ALICE, "alice", "Alice", null, "pic", null, null, updatedAt = 1))
        dao.posts.insert(post("old", createdAt = 1))
        dao.posts.insert(post("new", createdAt = 2))
        dao.posts.insert(post("reply", author = BOB, createdAt = 3, parent = "old"))

        val feed = dao.feed(PostFeed.Latest, "", 20)

        assertEquals(listOf("new", "old"), feed.map { it.id })
        assertEquals("Alice", feed.first().authorDisplayName)
        assertEquals(1, feed.last().replyCount)
        assertEquals(1, dao.feed(PostFeed.Latest, "", 1).size)
    }

    @Test
    fun deletedPostsDisappearAndNeverComeBack() = dbTest { dao ->
        assertTrue(dao.posts.insert(post("p")))
        assertFalse(dao.posts.insert(post("p")), "duplicate")
        dao.posts.markDeleted(listOf("p"), author = BOB)
        assertTrue(dao.posts.observePost("p", "").first() != null, "only the author can delete")
        assertTrue(dao.posts.insert(post("q", author = BOB)))

        dao.posts.markDeleted(listOf("p"), author = ALICE)
        assertNull(dao.posts.observePost("p", "").first())
        assertTrue(dao.feed(PostFeed.Latest, "", 20).all { it.id != "p" })
        assertFalse(dao.posts.insert(post("p2").copy(id = "p")), "tombstoned")
        assertEquals(ALICE, dao.posts.authorOf("p"))
    }

    @Test
    fun newestRevisionBecomesCurrent() = dbTest { dao ->
        dao.posts.insert(post("p"))
        dao.posts.addRevision(RevisionRecord("p", "p", 1, "v1", "[]", 1))
        dao.posts.addRevision(RevisionRecord("r3", "p", 3, "v3", "[]", 30))
        dao.posts.addRevision(RevisionRecord("r2", "p", 2, "v2", "[]", 20))

        val current = dao.posts.observePost("p", "").first()!!
        assertEquals("v3", current.content)
        assertEquals(3, current.revision)
        assertEquals(3, current.revisionCount)
        assertEquals(30, current.editedAt)
        assertEquals(listOf(3, 2, 1), dao.posts.observeRevisions("p").first().map { it.number })
    }

    @Test
    fun countersAndViewerFlags() = dbTest { dao ->
        dao.posts.insert(post("p"))
        dao.reactions.insert(reaction("l1", "p", BOB))
        dao.reactions.insert(reaction("l2", "p", BOB)) // second like by the same user counts once
        dao.reactions.insert(reaction("l3", "p", CAROL))
        dao.reactions.insert(reaction("r1", "p", BOB, ReactionTypes.REPOST))
        dao.bookmarks.replace(BOB, listOf("p"), createdAt = 1)

        val asBob = dao.posts.observePost("p", BOB).first()!!
        assertEquals(2, asBob.likeCount)
        assertEquals(1, asBob.repostCount)
        assertTrue(asBob.viewerLiked && asBob.viewerReposted && asBob.viewerBookmarked)
        assertFalse(dao.posts.observePost("p", CAROL).first()!!.viewerReposted)

        dao.reactions.delete(listOf("l3"), user = BOB)
        assertEquals(2, dao.posts.observePost("p", BOB).first()!!.likeCount, "Bob cannot delete Carol's like")
        dao.reactions.delete(dao.reactions.reactionIds("p", BOB, ReactionTypes.LIKE), user = BOB)
        assertEquals(1, dao.posts.observePost("p", BOB).first()!!.likeCount)
        assertFalse(dao.reactions.insert(reaction("l1", "p", BOB)), "deleted reaction stays deleted")
    }

    @Test
    fun threadIsFlattenedChronologicallyWithDepth() = dbTest { dao ->
        dao.posts.insert(post("root"))
        dao.posts.insert(post("a", BOB, createdAt = 2, parent = "root"))
        dao.posts.insert(post("a1", CAROL, createdAt = 3, parent = "a"))
        dao.posts.insert(post("b", CAROL, createdAt = 4, parent = "root"))

        val thread = dao.posts.observeThread("root", "").first()
        assertEquals(listOf("a" to 0, "a1" to 1, "b" to 0), thread.map { it.id to it.depth })
    }

    @Test
    fun followingFeedAndReplaceableListVersions() = dbTest { dao ->
        dao.posts.insert(post("mine", ALICE, 1))
        dao.posts.insert(post("bobs", BOB, 2))
        dao.posts.insert(post("carols", CAROL, 3))

        assertTrue(dao.follows.replace(ALICE, listOf(BOB), createdAt = 10))
        assertFalse(dao.follows.replace(ALICE, listOf(CAROL), createdAt = 9), "older list is ignored")

        assertEquals(listOf("bobs", "mine"), dao.feed(PostFeed.Following, ALICE, 20).map { it.id })
        assertTrue(dao.follows.observeIsFollowing(ALICE, BOB).first())
        assertEquals(1, dao.follows.observeFollowingCount(ALICE).first())
        assertEquals(10, dao.follows.version(ALICE))
    }

    @Test
    fun bookmarksKeepListOrderNewestFirst() = dbTest { dao ->
        dao.posts.insert(post("a"))
        dao.posts.insert(post("b"))
        dao.bookmarks.replace(ALICE, listOf("a", "b"), createdAt = 1)

        assertEquals(listOf("b", "a"), dao.feed(PostFeed.Bookmarks, ALICE, 20).map { it.id })
        assertEquals(listOf("a", "b"), dao.bookmarks.bookmarkIds(ALICE))
    }

    @Test
    fun relaysRoundTrip() = dbTest { dao ->
        val relays = listOf(RelayRecord("wss://a", true, true), RelayRecord("wss://b", true, false))
        dao.relayLists.replace(ALICE, relays, createdAt = 1)
        assertEquals(relays, dao.relayLists.observe(ALICE).first())
    }

    @Test
    fun activityCollectsRepliesQuotesReactionsAndFollows() = dbTest { dao ->
        dao.posts.insert(post("mine", ALICE, 1))
        dao.posts.insert(post("reply", BOB, 2, parent = "mine"))
        dao.posts.insert(post("quote", CAROL, 3, quote = "mine"))
        dao.posts.insert(post("selfReply", ALICE, 4, parent = "mine"))
        dao.reactions.insert(reaction("like", "mine", BOB, createdAt = 5))
        dao.follows.replace(CAROL, listOf(ALICE), createdAt = 6)

        val activity = dao.activity(ALICE)

        assertEquals(
            listOf(
                ActivityRecord("follow", CAROL, 6, null, null),
                ActivityRecord("like", BOB, 5, null, "mine"),
                ActivityRecord("quote", CAROL, 3, "quote", "mine"),
                ActivityRecord("reply", BOB, 2, "reply", "mine"),
            ),
            activity,
        )
    }

    @Test
    fun profilesKeepTheNewestVersion() = dbTest { dao ->
        dao.profiles.upsert(ProfileRecord(ALICE, "new", null, null, null, null, null, updatedAt = 10))
        dao.profiles.upsert(ProfileRecord(ALICE, "old", null, null, null, null, null, updatedAt = 5))
        assertEquals("new", dao.profiles.observe(ALICE).first()?.name)
    }

    @Test
    fun trendingInputsAndRankedFeed() = dbTest { dao ->
        dao.posts.insert(post("a", ALICE, 100))
        dao.posts.insert(post("b", ALICE, 200))
        dao.posts.insert(post("reply", BOB, 210, parent = "a"))
        dao.reactions.insert(reaction("l", "b", BOB))

        assertEquals(setOf("a", "b"), dao.trending.candidates(since = 50).map { it.postId }.toSet())
        assertEquals(setOf("a" to "reply", "b" to "like"), dao.trending.engagements(since = 50).map { it.postId to it.type }.toSet())

        dao.trending.replace(listOf(TrendingRecord(1, "b", 2.0), TrendingRecord(2, "a", 1.0)), computedAt = 300)
        assertEquals(listOf("b", "a"), dao.feed(PostFeed.Trending, "", 20).map { it.id })
        assertEquals(300, dao.trending.computedAt())
    }

    @Test
    fun signOutClearsUserDataButKeepsProfiles() = dbTest { dao ->
        dao.profiles.upsert(ProfileRecord(ALICE, "alice", null, null, null, null, null, 1))
        dao.posts.insert(post("p"))
        dao.maintenance.clearUserData()

        assertTrue(dao.feed(PostFeed.Latest, "", 20).isEmpty())
        assertEquals("alice", dao.profiles.observe(ALICE).first()?.name)
    }

    private companion object {
        const val ALICE = "alice"
        const val BOB = "bob"
        const val CAROL = "carol"
    }
}
