package com.yet.talk.database.dao.sql

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.TalkDatabase
import com.yet.talk.database.dao.SocialDao
import com.yet.talk.database.model.ActivityRecord
import com.yet.talk.database.model.RelayRecord
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@ContributesBinding(AppScope::class)
@Inject
class SqlSocialDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : SocialDao {

    override suspend fun replaceFollows(follower: String, followees: List<String>, createdAt: Long): Boolean =
        replaceList(follower, KIND_CONTACTS, createdAt) {
            socialQueries.deleteFollows(follower)
            followees.distinct().forEach { socialQueries.insertFollow(follower, it, createdAt) }
        }

    override fun observeFollowees(follower: String): Flow<List<String>> =
        holder.observeList(dispatchers.io) { socialQueries.selectFollowees(follower) }

    override fun observeIsFollowing(follower: String, followee: String): Flow<Boolean> =
        holder.observeOne(dispatchers.io) { socialQueries.isFollowing(follower, followee) }

    override fun observeFollowingCount(follower: String): Flow<Long> =
        holder.observeOne(dispatchers.io) { socialQueries.countFollowing(follower) }

    override suspend fun replaceBookmarks(owner: String, postIds: List<String>, createdAt: Long): Boolean =
        replaceList(owner, KIND_BOOKMARKS, createdAt) {
            socialQueries.deleteBookmarks(owner)
            postIds.distinct().forEachIndexed { position, id -> socialQueries.insertBookmark(owner, id, position.toLong()) }
        }

    override suspend fun bookmarkIds(owner: String): List<String> = withContext(dispatchers.io) {
        holder.get().socialQueries.selectBookmarkIds(owner).awaitAsList()
    }

    override suspend fun replaceRelays(owner: String, relays: List<RelayRecord>, createdAt: Long): Boolean =
        replaceList(owner, KIND_RELAYS, createdAt) {
            socialQueries.deleteRelays(owner)
            relays.forEach { socialQueries.insertRelay(owner, it.url, it.read.toLong(), it.write.toLong()) }
        }

    override fun observeRelays(owner: String): Flow<List<RelayRecord>> =
        holder.observeList(dispatchers.io) {
            socialQueries.selectRelays(owner) { url, read, write -> RelayRecord(url, read != 0L, write != 0L) }
        }

    override suspend fun listVersion(pubkey: String, kind: Int): Long? = withContext(dispatchers.io) {
        holder.get().socialQueries.selectReplaceableVersion(pubkey, kind.toLong()).awaitAsOneOrNull()
    }

    override fun observeActivity(me: String, limit: Int): Flow<List<ActivityRecord>> =
        holder.observeList(dispatchers.io) { socialQueries.selectActivity(me, limit.toLong(), ::ActivityRecord) }

    private suspend fun replaceList(pubkey: String, kind: Int, createdAt: Long, write: suspend TalkDatabase.() -> Unit): Boolean =
        withContext(dispatchers.io) {
            val database = holder.get()
            database.transactionWithResult {
                val stored = database.socialQueries.selectReplaceableVersion(pubkey, kind.toLong()).awaitAsOneOrNull()
                if (stored != null && stored >= createdAt) return@transactionWithResult false
                database.write()
                database.socialQueries.setReplaceableVersion(pubkey, kind.toLong(), createdAt)
                true
            }
        }

    private fun Boolean.toLong() = if (this) 1L else 0L

    private companion object {
        // Mirrors the Nostr kinds; the database does not depend on core:nostr.
        const val KIND_CONTACTS = 3
        const val KIND_RELAYS = 10002
        const val KIND_BOOKMARKS = 10003
    }
}
