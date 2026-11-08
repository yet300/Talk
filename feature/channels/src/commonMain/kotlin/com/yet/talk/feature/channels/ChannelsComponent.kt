package com.yet.talk.feature.channels

import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.channel.ChannelLevel
import com.yet.talk.domain.model.channel.LocationChannel
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.feature.channels.component.room.RoomComponent

/**
 * Location channels (`LocationChannels`): the first run (what they are, the location step, a nickname), then the
 * five levels around you with their people counts, Teleport by area code, and the open [room]. While this is open
 * and your location is known, you are announced at Region, Province and City (bitchat's presence).
 */
interface ChannelsComponent : BackHandlerOwner {
    val model: Value<Model>

    val room: Value<ChildSlot<*, RoomComponent>>

    /** First run: from "Location channels" to the location step. */
    fun onIntroContinue()

    /** The location step: asks the browser or device, computes the area codes on the device. */
    fun onAllowLocation()

    /** The location step, declined: on to the nickname, then Teleport only. */
    fun onTeleportInstead()

    fun onNicknameChanged(nickname: String)

    /** Saves the nickname; in the first run, this is Join. */
    fun onSaveNickname()

    fun onEditNickname()

    fun onOpen(channel: LocationChannel)

    /** Opens an area by its code at [level] (a longer code is cut to the level). */
    fun onTeleport(code: String, level: ChannelLevel)

    fun onShowTeleport(shown: Boolean)

    fun onBack()

    fun onErrorShown()

    data class Model(
        val step: Step,
        val location: Location,
        /** The five channels where you are; empty without a location. */
        val here: List<LocationChannel>,
        /** People in the last five minutes; null where nobody announces (Neighborhood, Block). */
        val people: Map<String, Int?>,
        val nickname: String,
        val nicknameDraft: String,
        val isTeleportOpen: Boolean,
        val error: DomainError?,
    )

    enum class Step { Loading, Intro, LocationStep, Nickname, List }

    enum class Location { Unknown, Locating, Known, Unavailable }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, onBack: () -> Unit): ChannelsComponent
    }
}
