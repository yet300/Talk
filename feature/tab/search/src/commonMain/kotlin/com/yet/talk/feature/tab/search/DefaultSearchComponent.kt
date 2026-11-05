package com.yet.talk.feature.tab.search

import com.app.common.decompose.asValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.feature.tab.search.mapper.stateToModel
import com.yet.talk.feature.tab.search.store.SearchStore
import com.yet.talk.feature.tab.search.store.SearchStoreFactory
import dev.zacsweers.metro.Inject

internal class DefaultSearchComponent(
    componentContext: ComponentContext,
    private val output: (SearchComponent.Output) -> Unit,
    storeFactory: SearchStoreFactory,
) : SearchComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create() }

    override val model: Value<SearchComponent.Model> = store.asValue().map(stateToModel)

    override fun onQueryChanged(query: String) = store.accept(SearchStore.Intent.ChangeQuery(query))

    override fun onLike(post: Post) = store.accept(SearchStore.Intent.Like(post))

    override fun onRepost(post: Post) = store.accept(SearchStore.Intent.Repost(post))

    override fun onBookmark(post: Post) = store.accept(SearchStore.Intent.Bookmark(post))

    override fun onReply(post: Post) = output(SearchComponent.Output.Reply(post.id))

    override fun onQuote(post: Post) = output(SearchComponent.Output.Quote(post.id))

    override fun onOpenPost(id: PostId) = output(SearchComponent.Output.OpenPost(id))

    override fun onOpenProfile(id: UserId) = output(SearchComponent.Output.OpenProfile(id))

    override fun onErrorShown() = store.accept(SearchStore.Intent.DismissError)
}

@Inject
internal class DefaultSearchComponentFactory(private val storeFactory: SearchStoreFactory) : SearchComponent.Factory {
    override fun invoke(componentContext: ComponentContext, output: (SearchComponent.Output) -> Unit): SearchComponent =
        DefaultSearchComponent(componentContext, output, storeFactory)
}
