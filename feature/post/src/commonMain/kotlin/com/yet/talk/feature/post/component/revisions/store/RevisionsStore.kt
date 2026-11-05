package com.yet.talk.feature.post.component.revisions.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.Revision

internal interface RevisionsStore : Store<RevisionsStore.Intent, RevisionsStore.State, Nothing> {

    data class State(
        val id: PostId,
        val revisions: List<Revision> = emptyList(),
        val selectedNumber: Int? = null,
        val isLoading: Boolean = false,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class Select(val number: Int) : Intent

        data object Refresh : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object Start : Action
    }

    sealed interface Msg {
        data class RevisionsChanged(val revisions: List<Revision>) : Msg

        data class Selected(val number: Int) : Msg

        data class Loading(val loading: Boolean) : Msg

        data class Failed(val error: DomainError?) : Msg
    }
}
