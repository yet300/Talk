package com.yet.talk.database.mapper

import com.yet.talk.database.model.PostRecord

@Suppress("UNUSED_PARAMETER") // `deleted` is filtered in SQL; every post query shares this column list.
internal fun postRecord(
    id: String, author: String, rootId: String?, parentId: String?, quoteId: String?, createdAt: Long, content: String,
    imagesJson: String, revision: Long, revisionCount: Long, editedAt: Long?, deleted: Long, publishState: String,
    authorName: String?, authorDisplayName: String?, authorPicture: String?, authorNip05: String?,
    likeCount: Long, repostCount: Long, replyCount: Long, parentAuthor: String?, parentAuthorName: String?,
    viewerLiked: Boolean, viewerReposted: Boolean, viewerBookmarked: Boolean,
) = PostRecord(
    id = id, author = author, rootId = rootId, parentId = parentId, quoteId = quoteId, createdAt = createdAt,
    content = content, imagesJson = imagesJson, revision = revision.toInt(), revisionCount = revisionCount.toInt(),
    editedAt = editedAt, publishState = publishState, authorName = authorName, authorDisplayName = authorDisplayName,
    authorPicture = authorPicture, authorNip05 = authorNip05, likeCount = likeCount, repostCount = repostCount,
    replyCount = replyCount, viewerLiked = viewerLiked, viewerReposted = viewerReposted, viewerBookmarked = viewerBookmarked,
    parentAuthor = parentAuthor, parentAuthorName = parentAuthorName,
)

/** [postRecord] plus the reposter of a Following row. */
@Suppress("LongParameterList")
internal fun followingRecord(
    id: String, author: String, rootId: String?, parentId: String?, quoteId: String?, createdAt: Long, content: String,
    imagesJson: String, revision: Long, revisionCount: Long, editedAt: Long?, deleted: Long, publishState: String,
    authorName: String?, authorDisplayName: String?, authorPicture: String?, authorNip05: String?,
    likeCount: Long, repostCount: Long, replyCount: Long, parentAuthor: String?, parentAuthorName: String?,
    viewerLiked: Boolean, viewerReposted: Boolean, viewerBookmarked: Boolean, repostedBy: String?, repostedByName: String?,
) = postRecord(
    id, author, rootId, parentId, quoteId, createdAt, content, imagesJson, revision, revisionCount, editedAt, deleted, publishState,
    authorName, authorDisplayName, authorPicture, authorNip05, likeCount, repostCount, replyCount, parentAuthor, parentAuthorName,
    viewerLiked, viewerReposted, viewerBookmarked,
).copy(repostedBy = repostedBy, repostedByName = repostedByName)
