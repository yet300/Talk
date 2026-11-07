package com.yet.talk.feature.tab.chats

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.feature.chat.component.conversation.ConversationComponent
import com.yet.talk.feature.chat.component.info.ChatInfoComponent
import com.yet.talk.feature.chat.component.newchat.NewChatComponent
import com.yet.talk.feature.chat.component.verify.VerifyComponent
import com.yet.talk.feature.channels.ChannelsComponent

/**
 * The Chats tab: the list (Chats · Requests) and, in [pane], the open conversation, its info, or New chat. Wide screens show
 * both side by side; a phone shows the pane over the list. Opening Chats publishes your inbox relays if you have none.
 */
interface ChatsComponent {
    val model: Value<Model>

    val pane: Value<ChildSlot<*, Pane>>

    fun onTabSelected(tab: Tab)

    fun onOpenChat(id: ChatId)

    fun onNewChat()

    /** The Location channels row at the top of the list. */
    fun onOpenChannels()

    fun onErrorShown()

    enum class Tab { Chats, Requests }

    data class Model(
        val tab: Tab,
        /** The chats of [tab], newest first. */
        val chats: List<Chat>,
        val requestCount: Int,
        val isLoading: Boolean,
        val error: DomainError?,
    )

    sealed class Pane {
        class Conversation(val id: ChatId, val component: ConversationComponent) : Pane()

        class NewChat(val component: NewChatComponent) : Pane()

        class Info(val id: ChatId, val component: ChatInfoComponent) : Pane()

        class Verify(val id: ChatId, val component: VerifyComponent) : Pane()

        class Channels(val component: ChannelsComponent) : Pane()
    }

    sealed interface Output {
        data class OpenProfile(val id: UserId) : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, output: (Output) -> Unit): ChatsComponent
    }
}
