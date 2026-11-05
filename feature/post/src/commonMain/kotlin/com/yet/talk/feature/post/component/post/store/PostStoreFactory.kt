package com.yet.talk.feature.post.component.post.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.repository.AccountRepository
import com.yet.talk.domain.repository.FollowRepository
import com.yet.talk.domain.usecase.social.ToggleFollow
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.usecase.post.ReplyToPost
import com.yet.talk.domain.usecase.social.ToggleBookmark
import com.yet.talk.domain.usecase.social.ToggleLike
import com.yet.talk.domain.usecase.social.ToggleRepost
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
internal class PostStoreFactory(
    private val storeFactory: StoreFactory,
    private val posts: PostRepository,
    private val accounts: AccountRepository,
    private val replyToPost: ReplyToPost,
    private val toggleLike: ToggleLike,
    private val toggleRepost: ToggleRepost,
    private val toggleBookmark: ToggleBookmark,
    private val follows: FollowRepository,
    private val toggleFollow: ToggleFollow,
) {
    fun create(id: PostId): PostStore =
        object : PostStore, Store<PostStore.Intent, PostStore.State, PostStore.Label> by storeFactory.create(
            name = "PostStore",
            initialState = PostStore.State(id = id),
            bootstrapper = SimpleBootstrapper(PostStore.Action.Start),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private inner class ExecutorImpl :
        CoroutineExecutor<PostStore.Intent, PostStore.Action, PostStore.State, PostStore.Msg, PostStore.Label>() {

        override fun executeAction(action: PostStore.Action) {
            val id = state().id
            scope.launch { posts.observePost(id).collect { dispatch(PostStore.Msg.PostChanged(it)) } }
            scope.launch { posts.observeThread(id).collect { dispatch(PostStore.Msg.RepliesChanged(it)) } }
            scope.launch { accounts.account.collect { dispatch(PostStore.Msg.ViewerChanged(it?.userId)) } }
            scope.launch {
                posts.observePost(id).map { it?.author?.id }.distinctUntilChanged()
                    .flatMapLatest { author -> author?.let(follows::observeIsFollowing) ?: flowOf(false) }
                    .collect { dispatch(PostStore.Msg.FollowingAuthorChanged(it)) }
            }
            scope.launch {
                posts.observePost(id).map { it?.quoteOf }.distinctUntilChanged()
                    .flatMapLatest { quoted -> quoted?.let(posts::observePost) ?: flowOf(null) }
                    .collect { dispatch(PostStore.Msg.QuotedChanged(it)) }
            }
            refresh()
        }

        override fun executeIntent(intent: PostStore.Intent) {
            when (intent) {
                PostStore.Intent.Refresh -> refresh()
                is PostStore.Intent.Like -> run { toggleLike(intent.post) }
                PostStore.Intent.FollowAuthor -> state().post?.author?.id?.let { author ->
                    val following = state().isFollowingAuthor
                    run { toggleFollow(author, following) }
                }
                is PostStore.Intent.Repost -> run { toggleRepost(intent.post) }
                is PostStore.Intent.Bookmark -> run { toggleBookmark(intent.post) }
                is PostStore.Intent.ChangeReply -> dispatch(PostStore.Msg.ReplyChanged(intent.text))
                PostStore.Intent.SendReply -> sendReply()
                PostStore.Intent.Delete -> delete()
                PostStore.Intent.DismissError -> dispatch(PostStore.Msg.Failed(null))
            }
        }

        private fun refresh() {
            dispatch(PostStore.Msg.Loading(true))
            scope.launch {
                val error = (posts.refreshPost(state().id).exceptionOrNull() ?: posts.refreshThread(state().id).exceptionOrNull())
                dispatch(PostStore.Msg.Loading(false))
                if (error != null) dispatch(PostStore.Msg.Failed(error as? DomainError))
            }
        }

        private fun sendReply() {
            val state = state()
            if (state.isReplying || state.replyText.isBlank()) return
            dispatch(PostStore.Msg.Replying(true))
            scope.launch {
                replyToPost(state.id, state.replyText)
                    .onSuccess { dispatch(PostStore.Msg.ReplySent) }
                    .onFailure { dispatch(PostStore.Msg.Failed(it as? DomainError)) }
                dispatch(PostStore.Msg.Replying(false))
            }
        }

        private fun delete() {
            scope.launch {
                posts.delete(state().id)
                    .onSuccess { publish(PostStore.Label.Deleted) }
                    .onFailure { dispatch(PostStore.Msg.Failed(it as? DomainError)) }
            }
        }

        /** Likes, reposts and bookmarks show up through the observed post; only failures need handling. */
        private fun run(action: suspend () -> Result<Unit>) {
            scope.launch { action().onFailure { dispatch(PostStore.Msg.Failed(it as? DomainError)) } }
        }
    }

    private object ReducerImpl : Reducer<PostStore.State, PostStore.Msg> {
        override fun PostStore.State.reduce(msg: PostStore.Msg): PostStore.State =
            when (msg) {
                is PostStore.Msg.PostChanged -> copy(post = msg.post)
                is PostStore.Msg.QuotedChanged -> copy(quoted = msg.post)
                is PostStore.Msg.RepliesChanged -> copy(replies = msg.replies)
                is PostStore.Msg.ViewerChanged -> copy(viewer = msg.viewer)
                is PostStore.Msg.FollowingAuthorChanged -> copy(isFollowingAuthor = msg.following)
                is PostStore.Msg.ReplyChanged -> copy(replyText = msg.text)
                is PostStore.Msg.Replying -> copy(isReplying = msg.replying)
                PostStore.Msg.ReplySent -> copy(replyText = "")
                is PostStore.Msg.Loading -> copy(isLoading = msg.loading)
                is PostStore.Msg.Failed -> copy(error = msg.error)
            }
    }
}
