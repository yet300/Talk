package com.yet.talk.feature.tab.home.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Post
import com.yet.talk.feature.tab.home.HomeComponent

internal interface HomeStore : Store<HomeStore.Intent, HomeStore.State, Nothing> {

    data class State(
        val feed: HomeComponent.Feed = HomeComponent.Feed.Following,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class SelectFeed(val feed: HomeComponent.Feed) : Intent

        data class Like(val post: Post) : Intent

        data class Repost(val post: Post) : Intent

        data class Bookmark(val post: Post) : Intent

        data object DismissError : Intent
    }

    sealed interface Msg {
        data class FeedSelected(val feed: HomeComponent.Feed) : Msg

        data class Failed(val error: DomainError?) : Msg
    }
}
