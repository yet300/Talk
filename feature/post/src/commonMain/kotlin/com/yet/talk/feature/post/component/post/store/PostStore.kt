package com.yet.talk.feature.post.component.post.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.ThreadReply
import com.yet.talk.domain.model.user.UserId

internal interface PostStore : Store<PostStore.Intent, PostStore.State, PostStore.Label> {

    data class State(
        val id: PostId,
        val post: Post? = null,
        val quoted: Post? = null,
        val replies: List<ThreadReply> = emptyList(),
        val viewer: UserId? = null,
        val isFollowingAuthor: Boolean = false,
        val replyText: String = "",
        val isReplying: Boolean = false,
        val isLoading: Boolean = false,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data object Refresh : Intent

        data class Like(val post: Post) : Intent

        data class Repost(val post: Post) : Intent

        data class Bookmark(val post: Post) : Intent

        data class ChangeReply(val text: String) : Intent

        data object SendReply : Intent

        data object Delete : Intent

        data object FollowAuthor : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object Start : Action
    }

    sealed interface Msg {
        data class PostChanged(val post: Post?) : Msg

        data class QuotedChanged(val post: Post?) : Msg

        data class RepliesChanged(val replies: List<ThreadReply>) : Msg

        data class ViewerChanged(val viewer: UserId?) : Msg

        data class FollowingAuthorChanged(val following: Boolean) : Msg

        data class ReplyChanged(val text: String) : Msg

        data class Replying(val replying: Boolean) : Msg

        data object ReplySent : Msg

        data class Loading(val loading: Boolean) : Msg

        data class Failed(val error: DomainError?) : Msg
    }

    sealed interface Label {
        data object Deleted : Label
    }
}
