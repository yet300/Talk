package com.yet.talk.database.dao

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.TalkDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@ContributesBinding(AppScope::class)
@Inject
class SqlProfileDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : ProfileDao {

    override suspend fun upsert(profile: ProfileRecord) = withContext(dispatchers.io) {
        val queries = holder.get().profileQueries
        holder.get().transaction {
            with(profile) {
                queries.insertIfAbsent(pubkey, name, displayName, about, picture, website, nip05, updatedAt)
                queries.updateIfNewer(name, displayName, about, picture, website, nip05, updatedAt, pubkey)
            }
        }
    }

    override fun observe(pubkey: String): Flow<ProfileRecord?> =
        holder.observeOneOrNull(dispatchers.io) {
            profileQueries.selectByPubkey(pubkey) { key, name, displayName, about, picture, website, nip05, updatedAt ->
                ProfileRecord(key, name, displayName, about, picture, website, nip05, updatedAt)
            }
        }
}

@ContributesBinding(AppScope::class)
@Inject
class SqlReactionDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : ReactionDao {

    override suspend fun insert(reaction: ReactionRecord): Boolean = withContext(dispatchers.io) {
        val database = holder.get()
        database.transactionWithResult {
            if (database.socialQueries.isTombstoned(reaction.eventId, reaction.user).awaitAsOne()) return@transactionWithResult false
            with(reaction) { database.socialQueries.insertReaction(eventId, postId, user, type, createdAt) } > 0
        }
    }

    override suspend fun reactionIds(postId: String, user: String, type: String): List<String> = withContext(dispatchers.io) {
        holder.get().socialQueries.selectReactionIds(postId, user, type).awaitAsList()
    }

    override suspend fun delete(eventIds: Collection<String>, user: String) = withContext(dispatchers.io) {
        if (eventIds.isEmpty()) return@withContext
        val database = holder.get()
        database.transaction {
            database.socialQueries.deleteReactions(eventIds, user)
            eventIds.forEach { database.socialQueries.insertTombstone(it, user) }
        }
    }
}

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

@ContributesBinding(AppScope::class)
@Inject
class SqlTrendingDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : TrendingDao {

    override suspend fun candidates(since: Long): List<TrendingCandidateRecord> = withContext(dispatchers.io) {
        holder.get().trendingQueries.selectTrendingCandidates(since, ::TrendingCandidateRecord).awaitAsList()
    }

    override suspend fun engagements(since: Long): List<EngagementRecord> = withContext(dispatchers.io) {
        holder.get().trendingQueries.selectTrendingEngagements(since) { postId, actor, type ->
            EngagementRecord(postId!!, actor, type)
        }.awaitAsList()
    }

    override suspend fun replace(entries: List<TrendingRecord>, computedAt: Long) = withContext(dispatchers.io) {
        val database = holder.get()
        database.transaction {
            database.trendingQueries.deleteTrending()
            entries.forEach { database.trendingQueries.insertTrending(it.rank.toLong(), it.postId, it.score, computedAt) }
        }
    }

    override suspend fun computedAt(): Long? = withContext(dispatchers.io) {
        holder.get().trendingQueries.selectTrendingComputedAt().awaitAsOneOrNull()?.MAX
    }
}

@ContributesBinding(AppScope::class)
@Inject
class SqlSyncDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : SyncDao {

    override suspend fun state(feedKey: String): SyncStateRecord? = withContext(dispatchers.io) {
        holder.get().trendingQueries.selectSyncState(feedKey) { _, endReached, syncedAt ->
            SyncStateRecord(endReached != 0L, syncedAt)
        }.awaitAsOneOrNull()
    }

    override suspend fun setState(feedKey: String, state: SyncStateRecord) = withContext(dispatchers.io) {
        holder.get().trendingQueries.setSyncState(feedKey, if (state.endReached) 1 else 0, state.syncedAt)
        Unit
    }
}

@ContributesBinding(AppScope::class)
@Inject
class SqlMaintenanceDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : MaintenanceDao {
    override suspend fun clearUserData() = withContext(dispatchers.io) {
        // Grouped statements return a lazy AsyncValue: nothing runs until it is awaited.
        holder.get().maintenanceQueries.clearUserData().await()
        Unit
    }
}
