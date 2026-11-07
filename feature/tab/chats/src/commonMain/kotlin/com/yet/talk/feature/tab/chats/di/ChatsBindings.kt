package com.yet.talk.feature.tab.chats.di

import com.yet.talk.feature.tab.chats.ChatsComponent
import com.yet.talk.feature.tab.chats.DefaultChatsComponentFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class ChatsBindings {
    @Binds
    internal abstract val DefaultChatsComponentFactory.bindChatsComponentFactory: ChatsComponent.Factory
}
