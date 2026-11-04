package com.yet.talk.data.mapper

import com.yet.talk.database.model.PostRecord
import com.yet.talk.database.model.PublishStates
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.PostStats
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.post.ThreadPosition
import com.yet.talk.domain.model.post.ViewerState
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.nostr.encoding.Nip19
import kotlin.time.Instant

internal fun PostRecord.toPost(): Post = Post(
    id = PostId(id),
    author = User(UserId(author), Nip19.npub(author), authorName, authorDisplayName, picture = authorPicture, nip05 = authorNip05),
    content = content,
    images = imagesFromJson(imagesJson),
    createdAt = Instant.fromEpochSeconds(createdAt),
    editedAt = editedAt?.let(Instant::fromEpochSeconds),
    revision = revision,
    revisionCount = revisionCount,
    thread = parentId?.let { parent -> ThreadPosition(PostId(rootId ?: parent), PostId(parent)) },
    quoteOf = quoteId?.let(::PostId),
    stats = PostStats(likes = likeCount, reposts = repostCount, replies = replyCount),
    viewer = ViewerState(liked = viewerLiked, reposted = viewerReposted, bookmarked = viewerBookmarked),
    publishState = when (publishState) {
        PublishStates.PENDING -> PublishState.Pending
        PublishStates.FAILED -> PublishState.Failed
        else -> PublishState.Published
    },
)
