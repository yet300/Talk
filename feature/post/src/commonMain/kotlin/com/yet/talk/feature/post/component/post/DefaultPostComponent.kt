package com.yet.talk.feature.post.component.post

import com.app.common.decompose.asValue
import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.feature.post.component.post.mapper.stateToModel
import com.yet.talk.feature.post.component.post.store.PostStore
import com.yet.talk.feature.post.component.post.store.PostStoreFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

internal class DefaultPostComponent(
    componentContext: ComponentContext,
    private val id: PostId,
    private val output: (PostComponent.Output) -> Unit,
    storeFactory: PostStoreFactory,
) : PostComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create(id) }

    init {
        coroutineScope().launch {
            store.labels.collect { label ->
                when (label) {
                    PostStore.Label.Deleted -> output(PostComponent.Output.Back)
                }
            }
        }
    }

    override val model: Value<PostComponent.Model> = store.asValue().map(stateToModel)

    override fun onRefresh() = store.accept(PostStore.Intent.Refresh)

    override fun onLike(post: Post) = store.accept(PostStore.Intent.Like(post))

    override fun onRepost(post: Post) = store.accept(PostStore.Intent.Repost(post))

    override fun onBookmark(post: Post) = store.accept(PostStore.Intent.Bookmark(post))

    override fun onReplyTextChanged(text: String) = store.accept(PostStore.Intent.ChangeReply(text))

    override fun onSendReply() = store.accept(PostStore.Intent.SendReply)

    override fun onDelete() = store.accept(PostStore.Intent.Delete)

    override fun onEdit() = output(PostComponent.Output.Edit(id))

    override fun onQuote(post: Post) = output(PostComponent.Output.Quote(post.id))

    override fun onReplyInEditor(post: Post) = output(PostComponent.Output.Reply(post.id))

    override fun onOpenPost(id: PostId) = output(PostComponent.Output.OpenPost(id))

    override fun onOpenAuthor(id: UserId) = output(PostComponent.Output.OpenProfile(id))

    override fun onOpenRevisions() = output(PostComponent.Output.OpenRevisions(id))

    override fun onBack() = output(PostComponent.Output.Back)

    override fun onErrorShown() = store.accept(PostStore.Intent.DismissError)
}

@Inject
internal class DefaultPostComponentFactory(private val storeFactory: PostStoreFactory) : PostComponent.Factory {
    override fun invoke(componentContext: ComponentContext, id: PostId, output: (PostComponent.Output) -> Unit): PostComponent =
        DefaultPostComponent(componentContext, id, output, storeFactory)
}
