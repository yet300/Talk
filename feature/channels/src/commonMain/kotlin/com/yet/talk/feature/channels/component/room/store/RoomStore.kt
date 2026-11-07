package com.yet.talk.feature.channels.component.room.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.channel.ChannelMessage
import com.yet.talk.domain.model.channel.LocationChannel
import com.yet.talk.domain.model.error.DomainError

internal interface RoomStore : Store<RoomStore.Intent, RoomStore.State, Nothing> {

    data class State(
        val channel: LocationChannel,
        val messages: List<ChannelMessage> = emptyList(),
        val people: Int? = null,
        val nickname: String = "",
        val text: String = "",
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class ChangeText(val text: String) : Intent

        data object Send : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object Join : Action
    }

    sealed interface Msg {
        data class RoomChanged(val messages: List<ChannelMessage>, val people: Int?) : Msg

        data class NicknameChanged(val nickname: String) : Msg

        data class TextChanged(val text: String) : Msg

        data class Failed(val error: DomainError?) : Msg
    }
}
