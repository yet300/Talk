package com.yet.talk.feature.tab.home

import androidx.paging.PagingData
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** The home timeline: KarpathyTalk's Following, Trending (here: Popular) and Latest feeds. */
interface HomeComponent {
    val model: Value<Model>

    /**
     * Following, Popular and Latest as pages to swipe between ([Feed.entries] in order). Each page keeps its own
     * posts and scroll position while its neighbours are shown.
     */
    val pages: Value<ChildPages<*, FeedPage>>

    /** Selects the feed's page (a tab, or the swipe that settled on it). */
    fun onFeedSelected(feed: Feed)

    fun onLike(post: Post)

    fun onRepost(post: Post)

    fun onBookmark(post: Post)

    fun onReply(post: Post)

    fun onQuote(post: Post)

    /** "Edited" on a card. */
    fun onOpenRevisions(post: Post)

    fun onOpenPost(id: PostId)

    fun onOpenAuthor(id: UserId)

    fun onCompose()

    fun onErrorShown()

    @Serializable
    enum class Feed { Following, Popular, Latest }

    /** One feed's posts, 20 a page; cached for the page's lifetime. */
    class FeedPage(val feed: Feed, val posts: Flow<PagingData<Post>>)

    data class Model(val feed: Feed, val error: DomainError?)

    sealed interface Output {
        data class OpenPost(val id: PostId) : Output

        data class OpenProfile(val id: UserId) : Output

        data class Reply(val parent: PostId) : Output

        data class Quote(val quoted: PostId) : Output

        data class OpenRevisions(val id: PostId) : Output

        data object NewPost : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, output: (Output) -> Unit): HomeComponent
    }
}
