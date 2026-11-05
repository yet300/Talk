package com.yet.talk.feature.tab.bookmarks

import androidx.paging.PagingData
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import kotlinx.coroutines.flow.Flow

/** Saved posts (the viewer's NIP-51 bookmark list), newest bookmark first. */
interface BookmarksComponent {
    val model: Value<Model>

    /** 20 a page; cached for the component's lifetime. */
    val posts: Flow<PagingData<Post>>

    fun onLike(post: Post)

    fun onRepost(post: Post)

    /** Removes the bookmark, or restores one just removed. */
    fun onBookmark(post: Post)

    fun onReply(post: Post)

    fun onQuote(post: Post)

    fun onOpenPost(id: PostId)

    fun onOpenAuthor(id: UserId)

    /** "Back to Home" in the empty state. */
    fun onBrowseHome()

    fun onErrorShown()

    data class Model(val error: DomainError?)

    sealed interface Output {
        data class OpenPost(val id: PostId) : Output

        data class OpenProfile(val id: UserId) : Output

        data class Reply(val parent: PostId) : Output

        data class Quote(val quoted: PostId) : Output

        data object BrowseHome : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, output: (Output) -> Unit): BookmarksComponent
    }
}
