package com.yet.talk.feature.channels.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.channel.ChannelLevel
import com.yet.talk.domain.model.channel.GeoPoint
import com.yet.talk.domain.model.channel.LocationChannel
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.feature.channels.ChannelsComponent.Location
import com.yet.talk.feature.channels.ChannelsComponent.Step

internal interface ChannelsStore : Store<ChannelsStore.Intent, ChannelsStore.State, ChannelsStore.Label> {

    data class State(
        val step: Step = Step.Loading,
        val location: Location = Location.Unknown,
        val point: GeoPoint? = null,
        val here: List<LocationChannel> = emptyList(),
        val people: Map<String, Int?> = emptyMap(),
        val nickname: String = "",
        val nicknameDraft: String = "",
        val isTeleportOpen: Boolean = false,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data object IntroContinue : Intent

        data object AllowLocation : Intent

        data object TeleportInstead : Intent

        data class ChangeNickname(val nickname: String) : Intent

        data object SaveNickname : Intent

        data object EditNickname : Intent

        data class Teleport(val code: String, val level: ChannelLevel) : Intent

        data class ShowTeleport(val shown: Boolean) : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object Load : Action
    }

    sealed interface Msg {
        data class StepChanged(val step: Step) : Msg

        data class LocationChanged(val location: Location, val point: GeoPoint?, val here: List<LocationChannel>) : Msg

        data class PeopleChanged(val people: Map<String, Int?>) : Msg

        data class NicknameChanged(val nickname: String) : Msg

        data class DraftChanged(val draft: String) : Msg

        data class TeleportShown(val shown: Boolean) : Msg

        data class Failed(val error: DomainError?) : Msg
    }

    sealed interface Label {
        data class Open(val channel: LocationChannel) : Label
    }
}
