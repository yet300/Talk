package com.yet.talk.feature.chat.component.info.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.Disappearing
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId

internal interface ChatInfoStore : Store<ChatInfoStore.Intent, ChatInfoStore.State, ChatInfoStore.Label> {

    data class State(
        val id: ChatId,
        val chat: Chat? = null,
        val members: List<User> = emptyList(),
        val blocked: Set<UserId> = emptySet(),
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class SetDisappearing(val disappearing: Disappearing) : Intent

        data class SetMuted(val muted: Boolean) : Intent

        data class SetBlocked(val blocked: Boolean) : Intent

        data object Leave : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object Load : Action
    }

    sealed interface Msg {
        data class ChatChanged(val chat: Chat?) : Msg

        data class MembersChanged(val members: List<User>) : Msg

        data class BlockedChanged(val blocked: Set<UserId>) : Msg

        data class Failed(val error: DomainError?) : Msg
    }

    sealed interface Label {
        data object Closed : Label
    }
}
