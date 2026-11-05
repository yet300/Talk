package com.yet.talk.feature.tab.bookmarks

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.app.common.decompose.asValue
import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.BookmarkRepository
import com.yet.talk.feature.tab.bookmarks.mapper.stateToModel
import com.yet.talk.feature.tab.bookmarks.store.BookmarksStore
import com.yet.talk.feature.tab.bookmarks.store.BookmarksStoreFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

internal class DefaultBookmarksComponent(
    componentContext: ComponentContext,
    private val output: (BookmarksComponent.Output) -> Unit,
    storeFactory: BookmarksStoreFactory,
    bookmarkRepository: BookmarkRepository,
) : BookmarksComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create() }

    override val model: Value<BookmarksComponent.Model> = store.asValue().map(stateToModel)

    override val posts: Flow<PagingData<Post>> = bookmarkRepository.bookmarks().cachedIn(coroutineScope())

    override fun onLike(post: Post) = store.accept(BookmarksStore.Intent.Like(post))

    override fun onRepost(post: Post) = store.accept(BookmarksStore.Intent.Repost(post))

    override fun onBookmark(post: Post) = store.accept(BookmarksStore.Intent.Bookmark(post))

    override fun onReply(post: Post) = output(BookmarksComponent.Output.Reply(post.id))

    override fun onQuote(post: Post) = output(BookmarksComponent.Output.Quote(post.id))

    override fun onOpenPost(id: PostId) = output(BookmarksComponent.Output.OpenPost(id))

    override fun onOpenAuthor(id: UserId) = output(BookmarksComponent.Output.OpenProfile(id))

    override fun onErrorShown() = store.accept(BookmarksStore.Intent.DismissError)
}

@Inject
internal class DefaultBookmarksComponentFactory(
    private val storeFactory: BookmarksStoreFactory,
    private val bookmarkRepository: BookmarkRepository,
) : BookmarksComponent.Factory {
    override fun invoke(componentContext: ComponentContext, output: (BookmarksComponent.Output) -> Unit): BookmarksComponent =
        DefaultBookmarksComponent(componentContext, output, storeFactory, bookmarkRepository)
}
