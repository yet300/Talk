package com.yet.talk.feature.post.component.post.mapper

import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.feature.post.component.post.store.PostStore

internal fun stateToModel(state: PostStore.State, shareLink: String): PostComponent.Model =
    PostComponent.Model(
        post = state.post,
        quoted = state.quoted,
        replies = state.replies,
        isMine = state.post != null && state.post.author.id == state.viewer,
        isFollowingAuthor = state.isFollowingAuthor,
        shareLink = shareLink,
        replyText = state.replyText,
        canSendReply = state.replyText.isNotBlank() && !state.isReplying,
        isReplying = state.isReplying,
        isLoading = state.isLoading && state.post == null,
        error = state.error,
    )
