package com.yet.talk.feature.channels

import com.app.common.decompose.asValue
import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.yet.talk.domain.model.channel.ChannelLevel
import com.yet.talk.domain.model.channel.LocationChannel
import com.yet.talk.feature.channels.component.room.RoomComponent
import com.yet.talk.feature.channels.store.ChannelsStore
import com.yet.talk.feature.channels.store.ChannelsStore.Intent
import com.yet.talk.feature.channels.store.ChannelsStoreFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

internal class DefaultChannelsComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit,
    storeFactory: ChannelsStoreFactory,
    private val roomFactory: RoomComponent.Factory,
) : ChannelsComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create() }

    private val navigation = SlotNavigation<RoomConfig>()

    override val room: Value<ChildSlot<*, RoomComponent>> =
        childSlot(source = navigation, serializer = RoomConfig.serializer(), handleBackButton = true) { config, context ->
            val level = ChannelLevel.entries.first { it.name == config.level }
            roomFactory(context, LocationChannel(config.geohash, level, config.teleport), navigation::dismiss)
        }

    init {
        coroutineScope().launch {
            store.labels.collect { if (it is ChannelsStore.Label.Open) onOpen(it.channel) }
        }
    }

    override val model: Value<ChannelsComponent.Model> = store.asValue().map {
        ChannelsComponent.Model(it.step, it.location, it.here, it.people, it.nickname, it.nicknameDraft, it.isTeleportOpen, it.error)
    }

    override fun onIntroContinue() = store.accept(Intent.IntroContinue)

    override fun onAllowLocation() = store.accept(Intent.AllowLocation)

    override fun onTeleportInstead() = store.accept(Intent.TeleportInstead)

    override fun onNicknameChanged(nickname: String) = store.accept(Intent.ChangeNickname(nickname))

    override fun onSaveNickname() = store.accept(Intent.SaveNickname)

    override fun onEditNickname() = store.accept(Intent.EditNickname)

    override fun onOpen(channel: LocationChannel) = navigation.activate(RoomConfig(channel.geohash, channel.level.name, channel.isTeleport))

    override fun onTeleport(code: String, level: ChannelLevel) = store.accept(Intent.Teleport(code, level))

    override fun onShowTeleport(shown: Boolean) = store.accept(Intent.ShowTeleport(shown))

    override fun onBack() = back()

    override fun onErrorShown() = store.accept(Intent.DismissError)

    @Serializable
    private data class RoomConfig(val geohash: String, val level: String, val teleport: Boolean)
}

@Inject
internal class DefaultChannelsComponentFactory(
    private val storeFactory: ChannelsStoreFactory,
    private val roomFactory: RoomComponent.Factory,
) : ChannelsComponent.Factory {
    override fun invoke(componentContext: ComponentContext, onBack: () -> Unit): ChannelsComponent =
        DefaultChannelsComponent(componentContext, onBack, storeFactory, roomFactory)
}
