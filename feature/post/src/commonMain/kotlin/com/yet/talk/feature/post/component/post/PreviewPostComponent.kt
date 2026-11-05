package com.yet.talk.feature.post.component.post

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId

/** For UI previews: a fixed model, actions do nothing. */
class PreviewPostComponent(
    model: PostComponent.Model = PostComponent.Model(
        post = null, quoted = null, replies = emptyList(), isMine = false, isFollowingAuthor = false,
        shareLink = "note1preview", replyText = "",
        canSendReply = false, isReplying = false, isLoading = true, error = null,
    ),
) : PostComponent {
    override val model: Value<PostComponent.Model> = MutableValue(model)

    override fun onRefresh() = Unit

    override fun onLike(post: Post) = Unit

    override fun onRepost(post: Post) = Unit

    override fun onBookmark(post: Post) = Unit

    override fun onReplyTextChanged(text: String) = Unit

    override fun onSendReply() = Unit

    override fun onDelete() = Unit

    override fun onFollowAuthor() = Unit

    override fun onEdit() = Unit

    override fun onQuote(post: Post) = Unit

    override fun onReplyInEditor(post: Post) = Unit

    override fun onOpenPost(id: PostId) = Unit

    override fun onOpenAuthor(id: UserId) = Unit

    override fun onOpenRevisions() = Unit

    override fun onBack() = Unit

    override fun onErrorShown() = Unit
}
