package com.yet.talk.feature.chat.di

import com.yet.talk.feature.chat.component.conversation.ConversationComponent
import com.yet.talk.feature.chat.component.conversation.DefaultConversationComponentFactory
import com.yet.talk.feature.chat.component.info.ChatInfoComponent
import com.yet.talk.feature.chat.component.info.DefaultChatInfoComponentFactory
import com.yet.talk.feature.chat.component.newchat.DefaultNewChatComponentFactory
import com.yet.talk.feature.chat.component.verify.DefaultVerifyComponentFactory
import com.yet.talk.feature.chat.component.verify.VerifyComponent
import com.yet.talk.feature.chat.component.newchat.NewChatComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class ChatBindings {
    @Binds
    internal abstract val DefaultConversationComponentFactory.bindConversationComponentFactory: ConversationComponent.Factory

    @Binds
    internal abstract val DefaultChatInfoComponentFactory.bindChatInfoComponentFactory: ChatInfoComponent.Factory

    @Binds
    internal abstract val DefaultVerifyComponentFactory.bindVerifyComponentFactory: VerifyComponent.Factory

    @Binds
    internal abstract val DefaultNewChatComponentFactory.bindNewChatComponentFactory: NewChatComponent.Factory
}
