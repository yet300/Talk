package com.yet.talk.feature.post.component.post

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.ThreadReply
import com.yet.talk.domain.model.user.UserId

/** One post (the reader): its markdown, the quoted post, actions, and the reply thread with a reply box. */
interface PostComponent {
    val model: Value<Model>

    fun onRefresh()

    fun onLike(post: Post)

    fun onRepost(post: Post)

    fun onBookmark(post: Post)

    fun onReplyTextChanged(text: String)

    fun onSendReply()

    /** Only for the viewer's own post; goes back once deleted. */
    fun onDelete()

    fun onEdit()

    fun onQuote(post: Post)

    /** Opens the full editor for a reply to [post] (the post itself or a reply in the thread). */
    fun onReplyInEditor(post: Post)

    fun onOpenPost(id: PostId)

    fun onOpenAuthor(id: UserId)

    fun onOpenRevisions()

    fun onBack()

    fun onErrorShown()

    data class Model(
        val post: Post?,
        val quoted: Post?,
        val replies: List<ThreadReply>,
        val isMine: Boolean,
        val replyText: String,
        val canSendReply: Boolean,
        val isReplying: Boolean,
        val isLoading: Boolean,
        val error: DomainError?,
    )

    sealed interface Output {
        data object Back : Output

        data class OpenPost(val id: PostId) : Output

        data class OpenProfile(val id: UserId) : Output

        data class OpenRevisions(val id: PostId) : Output

        data class Reply(val parent: PostId) : Output

        data class Quote(val quoted: PostId) : Output

        data class Edit(val id: PostId) : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, id: PostId, output: (Output) -> Unit): PostComponent
    }
}
