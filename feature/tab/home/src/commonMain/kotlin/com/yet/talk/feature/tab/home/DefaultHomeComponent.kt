package com.yet.talk.feature.tab.home

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.app.common.decompose.asValue
import com.app.common.decompose.combine
import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.Feed
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.repository.TrendingRepository
import com.yet.talk.feature.tab.home.mapper.stateToModel
import com.yet.talk.feature.tab.home.store.HomeStore
import com.yet.talk.feature.tab.home.store.HomeStoreFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

internal class DefaultHomeComponent(
    componentContext: ComponentContext,
    private val output: (HomeComponent.Output) -> Unit,
    storeFactory: HomeStoreFactory,
    private val postRepository: PostRepository,
    private val trendingRepository: TrendingRepository,
) : HomeComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create() }

    private val pagesNavigation = PagesNavigation<HomeComponent.Feed>()

    private val configPages: Value<ChildPages<HomeComponent.Feed, HomeComponent.FeedPage>> =
        childPages(
            source = pagesNavigation,
            serializer = HomeComponent.Feed.serializer(),
            initialPages = { Pages(items = HomeComponent.Feed.entries, selectedIndex = 0) },
            childFactory = { feed, context -> HomeComponent.FeedPage(feed, posts(feed).cachedIn(context.coroutineScope())) },
        )

    override val pages: Value<ChildPages<*, HomeComponent.FeedPage>> = configPages

    override val model: Value<HomeComponent.Model> =
        combine(store.asValue(), configPages) { state, pages -> stateToModel(state, pages.items[pages.selectedIndex].configuration) }

    private fun posts(feed: HomeComponent.Feed): Flow<PagingData<Post>> = when (feed) {
        HomeComponent.Feed.Following -> postRepository.feed(Feed.Following)
        HomeComponent.Feed.Popular -> trendingRepository.trending()
        HomeComponent.Feed.Latest -> postRepository.feed(Feed.Latest)
    }

    override fun onFeedSelected(feed: HomeComponent.Feed) = pagesNavigation.select(HomeComponent.Feed.entries.indexOf(feed))

    override fun onLike(post: Post) = store.accept(HomeStore.Intent.Like(post))

    override fun onRepost(post: Post) = store.accept(HomeStore.Intent.Repost(post))

    override fun onBookmark(post: Post) = store.accept(HomeStore.Intent.Bookmark(post))

    override fun onReply(post: Post) = output(HomeComponent.Output.Reply(post.id))

    override fun onQuote(post: Post) = output(HomeComponent.Output.Quote(post.id))

    override fun onOpenRevisions(post: Post) = output(HomeComponent.Output.OpenRevisions(post.id))

    override fun onOpenPost(id: PostId) = output(HomeComponent.Output.OpenPost(id))

    override fun onOpenAuthor(id: UserId) = output(HomeComponent.Output.OpenProfile(id))

    override fun onCompose() = output(HomeComponent.Output.NewPost)

    override fun onErrorShown() = store.accept(HomeStore.Intent.DismissError)
}

@Inject
internal class DefaultHomeComponentFactory(
    private val storeFactory: HomeStoreFactory,
    private val postRepository: PostRepository,
    private val trendingRepository: TrendingRepository,
) : HomeComponent.Factory {
    override fun invoke(componentContext: ComponentContext, output: (HomeComponent.Output) -> Unit): HomeComponent =
        DefaultHomeComponent(componentContext, output, storeFactory, postRepository, trendingRepository)
}
