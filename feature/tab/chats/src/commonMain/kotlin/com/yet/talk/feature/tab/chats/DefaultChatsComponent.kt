package com.yet.talk.feature.tab.chats

import com.app.common.decompose.asValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.feature.chat.component.conversation.ConversationComponent
import com.yet.talk.feature.chat.component.newchat.NewChatComponent
import com.yet.talk.feature.tab.chats.store.ChatsStore
import com.yet.talk.feature.tab.chats.store.ChatsStoreFactory
import dev.zacsweers.metro.Inject
import kotlinx.serialization.Serializable

internal class DefaultChatsComponent(
    componentContext: ComponentContext,
    private val output: (ChatsComponent.Output) -> Unit,
    storeFactory: ChatsStoreFactory,
    private val conversationFactory: ConversationComponent.Factory,
    private val newChatFactory: NewChatComponent.Factory,
) : ChatsComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create() }

    private val navigation = SlotNavigation<Config>()

    override val pane: Value<ChildSlot<*, ChatsComponent.Pane>> =
        childSlot(
            source = navigation,
            serializer = Config.serializer(),
            handleBackButton = true,
            childFactory = ::child,
        )

    override val model: Value<ChatsComponent.Model> = store.asValue().map { state ->
        ChatsComponent.Model(
            tab = state.tab,
            chats = state.chats.filter { it.isRequest == (state.tab == ChatsComponent.Tab.Requests) },
            requestCount = state.chats.count { it.isRequest },
            isLoading = state.isLoading,
            error = state.error,
        )
    }

    override fun onTabSelected(tab: ChatsComponent.Tab) = store.accept(ChatsStore.Intent.SelectTab(tab))

    override fun onOpenChat(id: ChatId) = navigation.activate(Config.Conversation(id.value))

    override fun onNewChat() = navigation.activate(Config.NewChat)

    override fun onErrorShown() = store.accept(ChatsStore.Intent.DismissError)

    private fun child(config: Config, context: ComponentContext): ChatsComponent.Pane = when (config) {
        is Config.Conversation -> ChatsComponent.Pane.Conversation(
            ChatId(config.id),
            conversationFactory(context, ChatId(config.id)) { event ->
                when (event) {
                    ConversationComponent.Output.Back, ConversationComponent.Output.Deleted -> navigation.dismiss()
                    is ConversationComponent.Output.OpenProfile -> output(ChatsComponent.Output.OpenProfile(event.id))
                }
            },
        )
        Config.NewChat -> ChatsComponent.Pane.NewChat(
            newChatFactory(context) { event ->
                when (event) {
                    NewChatComponent.Output.Back -> navigation.dismiss()
                    is NewChatComponent.Output.Open -> navigation.activate(Config.Conversation(event.id.value))
                }
            },
        )
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data class Conversation(val id: String) : Config

        @Serializable
        data object NewChat : Config
    }
}

@Inject
internal class DefaultChatsComponentFactory(
    private val storeFactory: ChatsStoreFactory,
    private val conversationFactory: ConversationComponent.Factory,
    private val newChatFactory: NewChatComponent.Factory,
) : ChatsComponent.Factory {
    override fun invoke(componentContext: ComponentContext, output: (ChatsComponent.Output) -> Unit): ChatsComponent =
        DefaultChatsComponent(componentContext, output, storeFactory, conversationFactory, newChatFactory)
}
