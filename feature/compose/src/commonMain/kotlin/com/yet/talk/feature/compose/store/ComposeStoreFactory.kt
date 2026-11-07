package com.yet.talk.feature.compose.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.usecase.media.UploadImage
import com.yet.talk.domain.usecase.post.EditPost
import com.yet.talk.domain.usecase.post.PublishPost
import com.yet.talk.domain.usecase.post.ReplyToPost
import com.yet.talk.feature.compose.ComposeMode
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Inject
internal class ComposeStoreFactory(
    private val storeFactory: StoreFactory,
    private val posts: PostRepository,
    private val publishPost: PublishPost,
    private val replyToPost: ReplyToPost,
    private val editPost: EditPost,
    private val uploadImage: UploadImage,
) {
    /** [draft] is the text restored after process death, if any. */
    fun create(mode: ComposeMode, draft: String?): ComposeStore =
        object : ComposeStore, Store<ComposeStore.Intent, ComposeStore.State, ComposeStore.Label> by storeFactory.create(
            name = "ComposeStore",
            initialState = ComposeStore.State(mode = mode, text = draft ?: (mode as? ComposeMode.Share)?.text.orEmpty(), prefilled = draft != null),
            bootstrapper = SimpleBootstrapper(ComposeStore.Action.LoadTarget),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl :
        CoroutineExecutor<ComposeStore.Intent, ComposeStore.Action, ComposeStore.State, ComposeStore.Msg, ComposeStore.Label>() {

        override fun executeAction(action: ComposeStore.Action) {
            val target = state().mode.target ?: return
            scope.launch { posts.observePost(target).filterNotNull().collect { dispatch(ComposeStore.Msg.TargetLoaded(it)) } }
            scope.launch { posts.refreshPost(target) }
        }

        override fun executeIntent(intent: ComposeStore.Intent) {
            when (intent) {
                is ComposeStore.Intent.ChangeText -> dispatch(ComposeStore.Msg.TextChanged(intent.text))
                is ComposeStore.Intent.ChangeView -> dispatch(ComposeStore.Msg.ViewChanged(intent.view))
                is ComposeStore.Intent.AttachImage -> attach(intent)
                is ComposeStore.Intent.RemoveImage -> dispatch(ComposeStore.Msg.ImageRemoved(intent.url))
                ComposeStore.Intent.Publish -> publish()
                ComposeStore.Intent.DismissError -> dispatch(ComposeStore.Msg.Failed(null))
            }
        }

        private fun attach(intent: ComposeStore.Intent.AttachImage) {
            dispatch(ComposeStore.Msg.Uploading(true))
            scope.launch {
                uploadImage(intent.file)
                    .onSuccess { dispatch(ComposeStore.Msg.ImageAdded(Image(url = it.url, mimeType = it.mimeType, width = it.width, height = it.height))) }
                    .onFailure { dispatch(ComposeStore.Msg.Failed(it as? DomainError)) }
                dispatch(ComposeStore.Msg.Uploading(false))
            }
        }

        private fun publish() {
            val state = state()
            if (state.isPublishing || state.isUploading) return
            dispatch(ComposeStore.Msg.Publishing(true))
            scope.launch {
                val result: Result<PostId> = when (val mode = state.mode) {
                    ComposeMode.New, is ComposeMode.Share -> publishPost(state.text, state.images)
                    is ComposeMode.Quote -> publishPost(state.text, state.images, quoteOf = mode.quoted)
                    is ComposeMode.Reply -> replyToPost(mode.parent, state.text, state.images)
                    is ComposeMode.Edit ->
                        editPost(mode.post, state.text, state.images, isReply = state.target?.thread != null).map { mode.post }
                }
                dispatch(ComposeStore.Msg.Publishing(false))
                result
                    .onSuccess { publish(ComposeStore.Label.Published(it)) }
                    .onFailure { dispatch(ComposeStore.Msg.Failed(it as? DomainError)) }
            }
        }
    }

    private object ReducerImpl : Reducer<ComposeStore.State, ComposeStore.Msg> {
        override fun ComposeStore.State.reduce(msg: ComposeStore.Msg): ComposeStore.State =
            when (msg) {
                is ComposeStore.Msg.TextChanged -> copy(text = msg.text)
                is ComposeStore.Msg.ViewChanged -> copy(view = msg.view)
                is ComposeStore.Msg.TargetLoaded ->
                    if (mode is ComposeMode.Edit && !prefilled) {
                        copy(target = msg.post, text = msg.post.content, images = msg.post.images, prefilled = true)
                    } else {
                        copy(target = msg.post)
                    }
                is ComposeStore.Msg.Uploading -> copy(isUploading = msg.uploading)
                // KarpathyTalk inserts the image's markdown into the post, and the image travels as an imeta tag.
                is ComposeStore.Msg.ImageAdded -> copy(
                    images = images + msg.image,
                    text = text.trimEnd().let { if (it.isEmpty()) "" else "$it\n\n" } + "![](${msg.image.url})\n",
                )
                is ComposeStore.Msg.ImageRemoved -> copy(
                    images = images.filterNot { it.url == msg.url },
                    text = text.replace("![](${msg.url})", "").trimEnd() + "\n",
                )
                is ComposeStore.Msg.Publishing -> copy(isPublishing = msg.publishing, error = if (msg.publishing) null else error)
                is ComposeStore.Msg.Failed -> copy(error = msg.error)
            }
    }
}
