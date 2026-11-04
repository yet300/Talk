package com.yet.talk.domain.repository

import com.yet.talk.domain.model.FeedItem
import com.yet.talk.domain.model.Image
import com.yet.talk.domain.model.Pager
import com.yet.talk.domain.model.Post
import com.yet.talk.domain.model.PostId
import com.yet.talk.domain.model.Revision
import com.yet.talk.domain.model.ThreadReply
import com.yet.talk.domain.model.UserId
import kotlinx.coroutines.flow.Flow

sealed interface Feed {
    /** Every top-level Talk post. */
    data object Latest : Feed

    /** Top-level posts and reposts from people the viewer follows. */
    data object Following : Feed

    data class UserPosts(val userId: UserId) : Feed

    data class UserReplies(val userId: UserId) : Feed
}

/**
 * Posts, replies and their revisions. Write methods expect content that already passed the
 * use-case rules ([com.yet.talk.domain.rule.ContentRules]).
 */
interface PostRepository {
    fun feed(feed: Feed): Pager<FeedItem>

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
}
