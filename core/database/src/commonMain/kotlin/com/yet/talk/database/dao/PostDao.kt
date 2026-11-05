package com.yet.talk.database.dao

import androidx.paging.PagingSource
import com.yet.talk.database.model.NewPostRecord
import com.yet.talk.database.model.PostFeed
import com.yet.talk.database.model.PostRecord
import com.yet.talk.database.model.RevisionRecord
import kotlinx.coroutines.flow.Flow

/**
 * Posts and their revisions: the local cache the UI reads. `viewer` is the signed-in pubkey, or ""
 * when signed out (all viewer flags are then false). Writes ignore events that were deleted (NIP-09).
 */
interface PostDao {
    /** False when the post already exists or was deleted. */
    suspend fun insert(post: NewPostRecord): Boolean

    /** Stores a revision and makes the newest one the post's current content. */
    suspend fun addRevision(revision: RevisionRecord)

    suspend fun setPublishState(id: String, state: String)

    /** Hides [author]'s posts among [ids] and remembers them so they are never stored again. */
    suspend fun markDeleted(ids: Collection<String>, author: String)

    suspend fun authorOf(id: String): String?

    /** The original signed event, for reposts and reactions. */
    suspend fun eventJson(id: String): String?

    fun observePost(id: String, viewer: String): Flow<PostRecord?>

    /** A factory of paging sources over [feed]; each source invalidates itself when the tables change. */
    suspend fun feedPages(feed: PostFeed, viewer: String): () -> PagingSource<Int, PostRecord>

    fun observeThread(rootId: String, viewer: String): Flow<List<PostRecord>>

    fun observeRevisions(postId: String): Flow<List<RevisionRecord>>

    /** Posts and replies whose markdown contains [text], newest first. */
    fun observeSearch(text: String, viewer: String, limit: Int): Flow<List<PostRecord>>
}
