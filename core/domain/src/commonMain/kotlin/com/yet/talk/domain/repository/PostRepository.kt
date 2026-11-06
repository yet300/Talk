package com.yet.talk.domain.repository

import androidx.paging.PagingData
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.Revision
import com.yet.talk.domain.model.post.ThreadReply
import kotlinx.coroutines.flow.Flow

/**
 * Posts, replies and their revisions. Write methods expect content that already passed the
 * use-case rules ([com.yet.talk.domain.rule.ContentRules]).
 */
interface PostRepository {
    /** Top-level posts (or, for [Feed.UserReplies], replies), newest first. Reposts are not timeline items, as in KarpathyTalk. */
    fun feed(feed: Feed): Flow<PagingData<Post>>

    /** Emits null while the post is unknown locally. */
    fun observePost(id: PostId): Flow<Post?>

    suspend fun refreshPost(id: PostId): Result<Unit>

    /** Replies below [id], depth-first in chronological order. */
    fun observeThread(id: PostId): Flow<List<ThreadReply>>

    suspend fun refreshThread(id: PostId): Result<Unit>

    /** Newest revision first. */
    fun observeRevisions(id: PostId): Flow<List<Revision>>

    suspend fun refreshRevisions(id: PostId): Result<Unit>

    suspend fun publish(content: String, images: List<Image>, quoteOf: PostId?): Result<PostId>

    suspend fun reply(parent: PostId, content: String, images: List<Image>): Result<PostId>

    suspend fun edit(id: PostId, content: String, images: List<Image>): Result<Unit>

    suspend fun delete(id: PostId): Result<Unit>

    /** Publishes again one of your posts marked Not sent ("Retry" on its card). */
    suspend fun retryPublish(id: PostId): Result<Unit>
}
