package com.yet.talk.feature.profile

import androidx.paging.PagingData
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.model.user.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** For UI previews: a fixed model and posts, actions do nothing. */
class PreviewProfileComponent(
    model: ProfileComponent.Model = ProfileComponent.Model(
        user = null, stats = UserStats(), isMe = true, isFollowing = false,
        tab = ProfileComponent.Tab.Posts, canGoBack = false, error = null,
    ),
    posts: List<Post> = emptyList(),
) : ProfileComponent {
    override val model: Value<ProfileComponent.Model> = MutableValue(model)

    override val posts: Flow<PagingData<Post>> = flowOf(PagingData.from(posts))

    override fun onTabSelected(tab: ProfileComponent.Tab) = Unit

    override fun onFollowToggle() = Unit

    override fun onRefresh() = Unit

    override fun onLike(post: Post) = Unit

    override fun onRepost(post: Post) = Unit

    override fun onBookmark(post: Post) = Unit

    override fun onReply(post: Post) = Unit

    override fun onQuote(post: Post) = Unit

    override fun onOpenPost(id: PostId) = Unit

    override fun onOpenAuthor(id: UserId) = Unit

    override fun onOpenSettings() = Unit

    override fun onBack() = Unit

    override fun onErrorShown() = Unit
}
