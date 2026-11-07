package com.yet.talk.feature.channels.component.room

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.channel.ChannelMessage
import com.yet.talk.domain.model.channel.LocationChannel
import com.yet.talk.domain.model.error.DomainError

/**
 * A channel room (`ChannelRoom`): a plain timeline of `nick#abcd` lines, "public and not kept", the people count, and
 * the composer. A teleport's messages carry the mark.
 */
interface RoomComponent {
    val model: Value<Model>

    fun onTextChanged(text: String)

    fun onSend()

    fun onBack()

    fun onErrorShown()

    data class Model(
        val channel: LocationChannel,
        val messages: List<ChannelMessage>,
        /** Null where nobody announces (Neighborhood, Block): "? people". */
        val people: Int?,
        val nickname: String,
        val text: String,
        val canSend: Boolean,
        val error: DomainError?,
    )

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, channel: LocationChannel, onBack: () -> Unit): RoomComponent
    }
}
