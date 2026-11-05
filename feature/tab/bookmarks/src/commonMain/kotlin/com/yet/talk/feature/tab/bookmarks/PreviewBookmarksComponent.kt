package com.yet.talk.feature.tab.bookmarks

import androidx.paging.PagingData
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** For UI previews: fixed posts, actions do nothing. */
class PreviewBookmarksComponent(posts: List<Post> = emptyList()) : BookmarksComponent {
    override val model: Value<BookmarksComponent.Model> = MutableValue(BookmarksComponent.Model(error = null))

    override val posts: Flow<PagingData<Post>> = flowOf(PagingData.from(posts))

    override fun onLike(post: Post) = Unit

    override fun onRepost(post: Post) = Unit

    override fun onBookmark(post: Post) = Unit

    override fun onReply(post: Post) = Unit

    override fun onQuote(post: Post) = Unit

    override fun onOpenPost(id: PostId) = Unit

    override fun onOpenAuthor(id: UserId) = Unit

    override fun onErrorShown() = Unit
}
