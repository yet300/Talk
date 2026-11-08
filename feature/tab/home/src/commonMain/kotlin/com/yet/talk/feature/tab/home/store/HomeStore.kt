package com.yet.talk.feature.tab.home.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.Post

internal interface HomeStore : Store<HomeStore.Intent, HomeStore.State, Nothing> {

    data class State(
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class Like(val post: Post) : Intent

        data class Repost(val post: Post) : Intent

        data class Bookmark(val post: Post) : Intent

        data object DismissError : Intent
    }

    sealed interface Msg {
        data class Failed(val error: DomainError?) : Msg
    }
}
