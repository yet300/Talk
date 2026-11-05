package com.yet.talk.feature.tab.search

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId

/** For UI previews: a fixed model, actions do nothing. */
class PreviewSearchComponent(
    model: SearchComponent.Model = SearchComponent.Model(
        query = "", users = emptyList(), posts = emptyList(), isSearching = false, isIdle = true, error = null,
    ),
) : SearchComponent {
    override val model: Value<SearchComponent.Model> = MutableValue(model)

    override fun onQueryChanged(query: String) = Unit

    override fun onLike(post: Post) = Unit

    override fun onRepost(post: Post) = Unit

    override fun onBookmark(post: Post) = Unit

    override fun onReply(post: Post) = Unit

    override fun onQuote(post: Post) = Unit

    override fun onOpenRevisions(post: Post) = Unit

    override fun onOpenPost(id: PostId) = Unit

    override fun onOpenProfile(id: UserId) = Unit

    override fun onErrorShown() = Unit
}
