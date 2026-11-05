package com.yet.talk.feature.tab.home

import androidx.paging.PagingData
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import kotlinx.coroutines.flow.Flow

/** The home timeline: KarpathyTalk's Following, Trending (here: Popular) and Latest feeds. */
interface HomeComponent {
    val model: Value<Model>

    /** The selected feed, 20 posts a page; cached for the component's lifetime. */
    val posts: Flow<PagingData<Post>>

    fun onFeedSelected(feed: Feed)

    fun onLike(post: Post)

    fun onRepost(post: Post)

    fun onBookmark(post: Post)

    fun onReply(post: Post)

    fun onQuote(post: Post)

    fun onOpenPost(id: PostId)

    fun onOpenAuthor(id: UserId)

    fun onCompose()

    fun onErrorShown()

    enum class Feed { Following, Popular, Latest }

    data class Model(val feed: Feed, val error: DomainError?)

    sealed interface Output {
        data class OpenPost(val id: PostId) : Output

        data class OpenProfile(val id: UserId) : Output

        data class Reply(val parent: PostId) : Output

        data class Quote(val quoted: PostId) : Output

        data object NewPost : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, output: (Output) -> Unit): HomeComponent
    }
}
