package com.yet.talk.database.dao

import kotlinx.coroutines.flow.Flow

/**
 * The local cache, the single source of truth for the UI. `viewer` is the signed-in pubkey, or ""
 * when signed out (all viewer flags are then false). Writes ignore events that were deleted (NIP-09).
 */
interface PostDao {
    /** False when the post already exists or was deleted. */
    suspend fun insert(post: NewPostRecord): Boolean

    /** Stores a revision and makes the newest one the post's current content. */
    suspend fun addRevision(revision: RevisionRecord)

    suspend fun setPublishState(id: String, state: String)

    /** Hides the posts and remembers their ids so they are never stored again. */
    suspend fun markDeleted(ids: Collection<String>)

    /** The original signed event, for reposts and reactions. */
    suspend fun eventJson(id: String): String?

    fun observePost(id: String, viewer: String): Flow<PostRecord?>

    fun observeFeed(feed: PostFeed, viewer: String, limit: Int): Flow<List<PostRecord>>

    fun observeThread(rootId: String, viewer: String): Flow<List<PostRecord>>

    fun observeRevisions(postId: String): Flow<List<RevisionRecord>>
}

interface ProfileDao {
    /** Keeps the newer of the stored and the given profile. */
    suspend fun upsert(profile: ProfileRecord)

    fun observe(pubkey: String): Flow<ProfileRecord?>
}

interface ReactionDao {
    /** False when it already exists or was deleted. */
    suspend fun insert(reaction: ReactionRecord): Boolean

    /** Event ids of [user]'s reactions of [type] on [postId], to delete them. */
    suspend fun reactionIds(postId: String, user: String, type: String): List<String>

    suspend fun delete(eventIds: Collection<String>)
}

/** Replaceable lists (NIP-02 contacts, NIP-51 bookmarks, NIP-65 relays); each replace applies only if [createdAt] is newer. */
interface SocialDao {
    suspend fun replaceFollows(follower: String, followees: List<String>, createdAt: Long): Boolean

    fun observeFollowees(follower: String): Flow<List<String>>

    fun observeIsFollowing(follower: String, followee: String): Flow<Boolean>

    fun observeFollowingCount(follower: String): Flow<Long>

    suspend fun replaceBookmarks(owner: String, postIds: List<String>, createdAt: Long): Boolean

    suspend fun bookmarkIds(owner: String): List<String>

    suspend fun replaceRelays(owner: String, relays: List<RelayRecord>, createdAt: Long): Boolean

    fun observeRelays(owner: String): Flow<List<RelayRecord>>

    /** created_at of the stored list of [kind] by [pubkey]; null when none is stored. */
    suspend fun listVersion(pubkey: String, kind: Int): Long?

    fun observeActivity(me: String, limit: Int): Flow<List<ActivityRecord>>
}

interface TrendingDao {
    suspend fun candidates(since: Long): List<TrendingCandidateRecord>

    suspend fun engagements(since: Long): List<EngagementRecord>

    suspend fun replace(entries: List<TrendingRecord>, computedAt: Long)

    suspend fun computedAt(): Long?
}

interface SyncDao {
    suspend fun state(feedKey: String): SyncStateRecord?

    suspend fun setState(feedKey: String, state: SyncStateRecord)
}

interface MaintenanceDao {
    /** Sign-out: everything but public profiles. */
    suspend fun clearUserData()
}
