package com.yet.talk.feature.chat.component.newchat.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId

internal interface NewChatStore : Store<NewChatStore.Intent, NewChatStore.State, NewChatStore.Label> {

    data class State(
        val query: String = "",
        val people: List<User> = emptyList(),
        val isSearching: Boolean = false,
        val checking: UserId? = null,
        val cannotReceive: Set<UserId> = emptySet(),
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class ChangeQuery(val query: String) : Intent

        data class Pick(val user: User) : Intent

        data object DismissError : Intent
    }

    sealed interface Msg {
        data class QueryChanged(val query: String) : Msg

        data class PeopleChanged(val people: List<User>) : Msg

        data class Searching(val searching: Boolean) : Msg

        data class Checking(val id: UserId?) : Msg

        data class CannotReceive(val id: UserId) : Msg

        data class Failed(val error: DomainError?) : Msg
    }

    sealed interface Label {
        data class Open(val id: ChatId) : Label
    }
}
