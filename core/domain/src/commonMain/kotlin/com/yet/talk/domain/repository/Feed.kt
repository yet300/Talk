package com.yet.talk.domain.repository

import com.yet.talk.domain.model.user.UserId

sealed interface Feed {
    /** Every top-level Talk post. */
    data object Latest : Feed

    /** Top-level posts by the viewer and the people they follow. */
    data object Following : Feed

    data class UserPosts(val userId: UserId) : Feed

    data class UserReplies(val userId: UserId) : Feed
}
