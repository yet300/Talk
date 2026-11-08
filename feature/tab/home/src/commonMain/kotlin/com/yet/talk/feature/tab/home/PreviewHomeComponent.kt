package com.yet.talk.feature.tab.home

import androidx.paging.PagingData
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import kotlinx.coroutines.flow.flowOf

/** For UI previews: a fixed model and posts, actions do nothing. */
class PreviewHomeComponent(
    model: HomeComponent.Model = HomeComponent.Model(feed = HomeComponent.Feed.Following, error = null),
    posts: List<Post> = emptyList(),
) : HomeComponent {
    override val model: Value<HomeComponent.Model> = MutableValue(model)

    override val pages: Value<ChildPages<*, HomeComponent.FeedPage>> = MutableValue(
        ChildPages(
            items = HomeComponent.Feed.entries.map { feed ->
                Child.Created(feed, HomeComponent.FeedPage(feed, flowOf(PagingData.from(if (feed == model.feed) posts else emptyList()))))
            },
            selectedIndex = HomeComponent.Feed.entries.indexOf(model.feed),
        ),
    )

    override fun onFeedSelected(feed: HomeComponent.Feed) = Unit

    override fun onLike(post: Post) = Unit

    override fun onRepost(post: Post) = Unit

    override fun onBookmark(post: Post) = Unit

    override fun onReply(post: Post) = Unit

    override fun onQuote(post: Post) = Unit

    override fun onOpenRevisions(post: Post) = Unit

    override fun onOpenPost(id: PostId) = Unit

    override fun onOpenAuthor(id: UserId) = Unit

    override fun onCompose() = Unit

    override fun onErrorShown() = Unit
}
