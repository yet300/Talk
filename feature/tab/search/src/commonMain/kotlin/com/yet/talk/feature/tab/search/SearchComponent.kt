package com.yet.talk.feature.tab.search

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId

/** Search people and posts. Matches the local cache as you type, then asks search relays (NIP-50). */
interface SearchComponent {
    val model: Value<Model>

    fun onQueryChanged(query: String)

    fun onLike(post: Post)

    fun onRepost(post: Post)

    fun onBookmark(post: Post)

    fun onReply(post: Post)

    fun onQuote(post: Post)

    fun onOpenPost(id: PostId)

    fun onOpenProfile(id: UserId)

    fun onErrorShown()

    data class Model(
        val query: String,
        val users: List<User>,
        val posts: List<Post>,
        val isSearching: Boolean,
        /** The query is too short to search, so nothing is shown. */
        val isIdle: Boolean,
        val error: DomainError?,
    )

    sealed interface Output {
        data class OpenPost(val id: PostId) : Output

        data class OpenProfile(val id: UserId) : Output

        data class Reply(val parent: PostId) : Output

        data class Quote(val quoted: PostId) : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, output: (Output) -> Unit): SearchComponent
    }
}
