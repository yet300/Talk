package com.yet.talk.feature.profile

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.app.common.decompose.asValue
import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.repository.Feed
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.feature.profile.mapper.stateToModel
import com.yet.talk.feature.profile.store.ProfileStore
import com.yet.talk.feature.profile.store.ProfileStoreFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultProfileComponent(
    componentContext: ComponentContext,
    userId: UserId?,
    private val output: (ProfileComponent.Output) -> Unit,
    storeFactory: ProfileStoreFactory,
    postRepository: PostRepository,
) : ProfileComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create(userId) }

    override val model: Value<ProfileComponent.Model> = store.asValue().map(stateToModel)

    override val posts: Flow<PagingData<Post>> = store.states
        .map { it.userId to it.tab }
        .distinctUntilChanged()
        .flatMapLatest { (id, tab) ->
            when {
                id == null -> flowOf(PagingData.empty())
                tab == ProfileComponent.Tab.Posts -> postRepository.feed(Feed.UserPosts(id))
                else -> postRepository.feed(Feed.UserReplies(id))
            }
        }
        .cachedIn(coroutineScope())

    override fun onTabSelected(tab: ProfileComponent.Tab) = store.accept(ProfileStore.Intent.SelectTab(tab))

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

    override fun onBack() = output(ProfileComponent.Output.Back)

    override fun onErrorShown() = store.accept(ProfileStore.Intent.DismissError)
}

@Inject
internal class DefaultProfileComponentFactory(
    private val storeFactory: ProfileStoreFactory,
    private val postRepository: PostRepository,
) : ProfileComponent.Factory {
    override fun invoke(componentContext: ComponentContext, userId: UserId?, output: (ProfileComponent.Output) -> Unit): ProfileComponent =
        DefaultProfileComponent(componentContext, userId, output, storeFactory, postRepository)
}
