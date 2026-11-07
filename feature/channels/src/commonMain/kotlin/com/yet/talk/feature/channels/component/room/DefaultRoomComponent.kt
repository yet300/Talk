package com.yet.talk.feature.channels.component.room

import com.app.common.decompose.asValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.yet.talk.domain.model.channel.LocationChannel
import com.yet.talk.domain.rule.ContentRules
import com.yet.talk.feature.channels.component.room.store.RoomStore.Intent
import com.yet.talk.feature.channels.component.room.store.RoomStoreFactory
import dev.zacsweers.metro.Inject

internal class DefaultRoomComponent(
    componentContext: ComponentContext,
    channel: LocationChannel,
    private val back: () -> Unit,
    storeFactory: RoomStoreFactory,
) : RoomComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore(key = "room-${channel.geohash}") { storeFactory.create(channel) }

    override val model: Value<RoomComponent.Model> = store.asValue().map { state ->
        RoomComponent.Model(
            channel = state.channel,
            messages = state.messages,
            people = state.people,
            nickname = state.nickname,
            text = state.text,
            canSend = state.text.isNotBlank() && state.text.length <= ContentRules.MAX_REPLY_LENGTH,
            error = state.error,
        )
    }

    override fun onTextChanged(text: String) = store.accept(Intent.ChangeText(text))

    override fun onSend() = store.accept(Intent.Send)

    override fun onBack() = back()

    override fun onErrorShown() = store.accept(Intent.DismissError)
}

@Inject
internal class DefaultRoomComponentFactory(private val storeFactory: RoomStoreFactory) : RoomComponent.Factory {
    override fun invoke(componentContext: ComponentContext, channel: LocationChannel, onBack: () -> Unit): RoomComponent =
        DefaultRoomComponent(componentContext, channel, onBack, storeFactory)
}
