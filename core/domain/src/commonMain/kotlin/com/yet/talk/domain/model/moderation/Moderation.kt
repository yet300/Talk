package com.yet.talk.domain.model.moderation

import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId

/**
 * What Talk softens, from the viewer's network (themselves and whom they follow): avatars and banners of people
 * outside it are blurred, and posts that someone in it reported (NIP-56) are hidden behind a row.
 */
data class Moderation(
    val me: UserId? = null,
    val following: Set<UserId> = emptySet(),
    val hiddenPosts: Set<PostId> = emptySet(),
    /** People the viewer blocked (private entries of their NIP-51 mute list): their posts and chats are hidden. */
    val blocked: Set<UserId> = emptySet(),
) {
    /** True until the viewer is known, so nothing blurs while signing in. */
    fun isInNetwork(id: UserId): Boolean = me == null || id == me || id in following

    /** Why a post is hidden behind a row, if it is. */
    fun hiddenReason(post: PostId, author: UserId): HiddenReason? = when {
        author in blocked -> HiddenReason.Blocked
        post in hiddenPosts -> HiddenReason.Reported
        else -> null
    }
}

enum class HiddenReason { Reported, Blocked }

/** NIP-56 report types Talk offers (`Report` sheet). */
enum class ReportReason(val nip56: String) {
    Nudity("nudity"),
    Spam("spam"),
    Illegal("illegal"),
    Impersonation("impersonation"),
    Other("other"),
}

sealed interface ReportTarget {
    val author: UserId

    data class Post(val id: PostId, override val author: UserId) : ReportTarget

    data class Profile(override val author: UserId) : ReportTarget
}
