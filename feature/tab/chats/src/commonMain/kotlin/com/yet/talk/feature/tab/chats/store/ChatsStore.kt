package com.yet.talk.feature.tab.chats.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.feature.tab.chats.ChatsComponent

internal interface ChatsStore : Store<ChatsStore.Intent, ChatsStore.State, Nothing> {

    data class State(
        val tab: ChatsComponent.Tab = ChatsComponent.Tab.Chats,
        val chats: List<Chat> = emptyList(),
        val isLoading: Boolean = true,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class SelectTab(val tab: ChatsComponent.Tab) : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object Load : Action
    }

    sealed interface Msg {
        data class TabSelected(val tab: ChatsComponent.Tab) : Msg

        data class ChatsChanged(val chats: List<Chat>) : Msg

        data class Failed(val error: DomainError?) : Msg
    }
}
