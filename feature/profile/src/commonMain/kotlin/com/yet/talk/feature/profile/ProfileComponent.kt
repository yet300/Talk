package com.yet.talk.feature.profile

import androidx.paging.PagingData
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.model.user.UserStats
import kotlinx.coroutines.flow.Flow

/** A user's profile with their posts or replies; the viewer's own profile when opened without a user. */
interface ProfileComponent {
    val model: Value<Model>

    /** The selected tab's posts, 20 a page; cached for the component's lifetime. */
    val posts: Flow<PagingData<Post>>

    fun onTabSelected(tab: Tab)

    fun onFollowToggle()

    fun onRefresh()

    fun onLike(post: Post)

    fun onRepost(post: Post)

    fun onBookmark(post: Post)

    fun onReply(post: Post)

    fun onQuote(post: Post)

    fun onOpenPost(id: PostId)

    fun onOpenAuthor(id: UserId)

    fun onOpenSettings()

    fun onBack()

    fun onErrorShown()

    enum class Tab { Posts, Replies }

    data class Model(
        val user: User?,
        val stats: UserStats,
        val isMe: Boolean,
        val isFollowing: Boolean,
        val tab: Tab,
        /** False for the Profile tab, which has no back. */
        val canGoBack: Boolean,
        val error: DomainError?,
    )

    sealed interface Output {
        data object Back : Output

        data class OpenPost(val id: PostId) : Output

        data class OpenProfile(val id: UserId) : Output

        data class Reply(val parent: PostId) : Output

        data class Quote(val quoted: PostId) : Output

        data object OpenSettings : Output
    }

    fun interface Factory {
        /** [userId] null: the signed-in user's own profile (the Profile tab). */
        operator fun invoke(componentContext: ComponentContext, userId: UserId?, output: (Output) -> Unit): ProfileComponent
    }
}
