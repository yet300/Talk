package com.yet.talk.feature.profile

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
import com.arkivanov.decompose.router.pages.navigate
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.BookmarkRepository
import com.yet.talk.domain.repository.Feed
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.feature.profile.mapper.stateToModel
import com.yet.talk.feature.profile.store.ProfileStore
import com.yet.talk.feature.profile.store.ProfileStoreFactory
import com.yet.talk.feature.profile.store.isMe
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultProfileComponent(
    componentContext: ComponentContext,
    userId: UserId?,
    private val output: (ProfileComponent.Output) -> Unit,
    storeFactory: ProfileStoreFactory,
    private val postRepository: PostRepository,
    private val bookmarkRepository: BookmarkRepository,
) : ProfileComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create(userId) }

    private val pagesNavigation = PagesNavigation<ProfileComponent.Tab>()

    private val configPages: Value<ChildPages<ProfileComponent.Tab, ProfileComponent.TabPage>> =
        childPages(
            source = pagesNavigation,
            serializer = ProfileComponent.Tab.serializer(),
            initialPages = { Pages(items = tabsFor(store.state), selectedIndex = 0) },
            childFactory = { tab, context -> ProfileComponent.TabPage(tab, posts(tab).cachedIn(context.coroutineScope())) },
        )

    override val pages: Value<ChildPages<*, ProfileComponent.TabPage>> = configPages

    /** The selected tab comes from the pages, the one place it is kept. */
    override val model: Value<ProfileComponent.Model> = combine(store.asValue(), configPages) { state, pages ->
        stateToModel(state, pages.items.getOrNull(pages.selectedIndex)?.configuration ?: ProfileComponent.Tab.Posts)
    }

    init {
        // Bookmarks (the viewer's own list) joins the pages once the profile turns out to be your own, and leaves
        // if it is not; the selected tab stays selected when it still exists.
        coroutineScope().launch {
            store.states.map(::tabsFor).distinctUntilChanged().collect { tabs ->
                pagesNavigation.navigate { pages ->
                    if (pages.items == tabs) pages else {
                        val selected = pages.items.getOrNull(pages.selectedIndex)
                        Pages(items = tabs, selectedIndex = tabs.indexOf(selected).coerceAtLeast(0))
                    }
                }
            }
        }
    }

    private fun tabsFor(state: ProfileStore.State): List<ProfileComponent.Tab> =
        if (state.requested == null || state.isMe) ProfileComponent.Tab.entries else listOf(ProfileComponent.Tab.Posts, ProfileComponent.Tab.Replies)

    private fun posts(tab: ProfileComponent.Tab): Flow<PagingData<Post>> = store.states
        .map { it.userId to it.isMe }
        .distinctUntilChanged()
        .flatMapLatest { (id, isMe) ->
            when {
                id == null -> flowOf(PagingData.empty())
                tab == ProfileComponent.Tab.Posts -> postRepository.feed(Feed.UserPosts(id))
                tab == ProfileComponent.Tab.Replies -> postRepository.feed(Feed.UserReplies(id))
                isMe -> bookmarkRepository.bookmarks()
                else -> flowOf(PagingData.empty())
            }
        }

    override fun onTabSelected(tab: ProfileComponent.Tab) {
        val index = configPages.value.items.indexOfFirst { it.configuration == tab }
        if (index >= 0) pagesNavigation.select(index)
    }

    override fun onFollowToggle() = store.accept(ProfileStore.Intent.ToggleFollow)

    override fun onRefresh() = store.accept(ProfileStore.Intent.Refresh)

    override fun onLike(post: Post) = store.accept(ProfileStore.Intent.Like(post))

    override fun onRepost(post: Post) = store.accept(ProfileStore.Intent.Repost(post))

    override fun onBookmark(post: Post) = store.accept(ProfileStore.Intent.Bookmark(post))

    override fun onReply(post: Post) = output(ProfileComponent.Output.Reply(post.id))

    override fun onQuote(post: Post) = output(ProfileComponent.Output.Quote(post.id))

    override fun onOpenRevisions(post: Post) = output(ProfileComponent.Output.OpenRevisions(post.id))

    override fun onOpenPost(id: PostId) = output(ProfileComponent.Output.OpenPost(id))

    override fun onOpenAuthor(id: UserId) = output(ProfileComponent.Output.OpenProfile(id))

    override fun onOpenSettings() = output(ProfileComponent.Output.OpenSettings)

    override fun onEditProfile() = output(ProfileComponent.Output.EditProfile)

    override fun onBack() = output(ProfileComponent.Output.Back)

    override fun onErrorShown() = store.accept(ProfileStore.Intent.DismissError)
}

@Inject
internal class DefaultProfileComponentFactory(
    private val storeFactory: ProfileStoreFactory,
    private val postRepository: PostRepository,
    private val bookmarkRepository: BookmarkRepository,
) : ProfileComponent.Factory {
    override fun invoke(componentContext: ComponentContext, userId: UserId?, output: (ProfileComponent.Output) -> Unit): ProfileComponent =
        DefaultProfileComponent(componentContext, userId, output, storeFactory, postRepository, bookmarkRepository)
}
