package com.yet.talk.feature.channels.di

import com.yet.talk.feature.channels.ChannelsComponent
import com.yet.talk.feature.channels.DefaultChannelsComponentFactory
import com.yet.talk.feature.channels.component.room.DefaultRoomComponentFactory
import com.yet.talk.feature.channels.component.room.RoomComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class ChannelsBindings {
    @Binds
    internal abstract val DefaultChannelsComponentFactory.bindChannelsComponentFactory: ChannelsComponent.Factory

    @Binds
    internal abstract val DefaultRoomComponentFactory.bindRoomComponentFactory: RoomComponent.Factory
}
