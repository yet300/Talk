package com.yet.talk.data

import com.yet.talk.database.dao.PostRecord
import com.yet.talk.database.dao.ProfileRecord
import com.yet.talk.database.dao.PublishStates
import com.yet.talk.database.dao.RevisionRecord
import com.yet.talk.domain.model.Image
import com.yet.talk.domain.model.Post
import com.yet.talk.domain.model.PostId
import com.yet.talk.domain.model.PostStats
import com.yet.talk.domain.model.PublishState
import com.yet.talk.domain.model.Revision
import com.yet.talk.domain.model.ThreadPosition
import com.yet.talk.domain.model.User
import com.yet.talk.domain.model.UserId
import com.yet.talk.domain.model.ViewerState
import com.yet.talk.nostr.Nip19
import com.yet.talk.nostr.nip.Imeta
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** Images are stored as JSON in a TEXT column. */
@Serializable
internal data class StoredImage(
    val url: String,
    val mimeType: String? = null,
    val sha256: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val alt: String? = null,
)

private val json = Json { ignoreUnknownKeys = true }
private val imagesSerializer = ListSerializer(StoredImage.serializer())

internal fun List<Imeta>.toImagesJson(): String =
    json.encodeToString(imagesSerializer, map { StoredImage(it.url, it.mimeType, it.sha256, it.width, it.height, it.alt) })

internal fun List<Image>.toImeta(): List<Imeta> = map { Imeta(it.url, it.mimeType, width = it.width, height = it.height, alt = it.alt) }

internal fun imagesFromJson(value: String): List<Image> =
    runCatching { json.decodeFromString(imagesSerializer, value) }.getOrDefault(emptyList())
        .map { Image(it.url, it.mimeType, it.width, it.height, it.alt) }

internal fun userOf(pubkey: String, profile: ProfileRecord?): User = User(
    id = UserId(pubkey),
    npub = Nip19.npub(pubkey),
    name = profile?.name,
    displayName = profile?.displayName,
    about = profile?.about,
    picture = profile?.picture,
    website = profile?.website,
    nip05 = profile?.nip05,
)

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

internal fun RevisionRecord.toRevision() = Revision(PostId(postId), number, content, Instant.fromEpochSeconds(createdAt))
