package com.yet.talk.feature.tab.chats

import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
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
import kotlinx.serialization.Serializable

/**
 * The Chats tab: the list (Chats · Requests) and, in [pane], the open conversation, its info, or New chat. Wide screens show
 * both side by side; a phone shows the pane over the list. Opening Chats publishes your inbox relays if you have none.
 */
interface ChatsComponent : BackHandlerOwner {
    val model: Value<Model>

    val pane: Value<ChildSlot<*, Pane>>

    /** Chats and Requests as pages to swipe between ([Tab.entries] in order). */
    val pages: Value<ChildPages<*, ListPage>>

    /** Selects the tab's page (a tab, or the swipe that settled on it). */
    fun onTabSelected(tab: Tab)

    fun onOpenChat(id: ChatId)

    fun onNewChat()

    fun onErrorShown()

    @Serializable
    enum class Tab { Chats, Requests }

    /** One page of the list: the chats of [tab], newest first. */
    class ListPage(val tab: Tab, val chats: Value<List<Chat>>)

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
    }

    sealed interface Output {
        data class OpenProfile(val id: UserId) : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, output: (Output) -> Unit): ChatsComponent
    }
}
