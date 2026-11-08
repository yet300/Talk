package com.yet.talk.feature.profile

import androidx.paging.PagingData
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.model.user.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** A user's profile with their posts or replies; the viewer's own profile when opened without a user. */
interface ProfileComponent {
    val model: Value<Model>

    /**
     * The tabs as pages to swipe between: Posts and Replies, and Bookmarks on your own profile once that is known.
     * Each page keeps its own posts and scroll position.
     */
    val pages: Value<ChildPages<*, TabPage>>

    fun onTabSelected(tab: Tab)

    fun onFollowToggle()

    fun onRefresh()

    fun onLike(post: Post)

    fun onRepost(post: Post)

    fun onBookmark(post: Post)

    fun onReply(post: Post)

    fun onQuote(post: Post)

    /** "Edited" on a card. */
    fun onOpenRevisions(post: Post)

    fun onOpenPost(id: PostId)

    fun onOpenAuthor(id: UserId)

    fun onOpenSettings()

    /** "Edit profile" on your own profile. */
    fun onEditProfile()

    fun onBack()

    fun onErrorShown()

    /** [Tab.Bookmarks] exists only on your own profile (the phone shows it there instead of a nav item). */
    @Serializable
    enum class Tab { Posts, Replies, Bookmarks }

    /** One tab's posts, 20 a page; cached for the page's lifetime. */
    class TabPage(val tab: Tab, val posts: Flow<PagingData<Post>>)

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

        data class OpenRevisions(val id: PostId) : Output

        data object OpenSettings : Output

        data object EditProfile : Output
    }

    fun interface Factory {
        /** [userId] null: the signed-in user's own profile (the Profile tab). */
        operator fun invoke(componentContext: ComponentContext, userId: UserId?, output: (Output) -> Unit): ProfileComponent
    }
}
