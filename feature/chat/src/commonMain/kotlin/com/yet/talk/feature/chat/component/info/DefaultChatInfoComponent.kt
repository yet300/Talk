package com.yet.talk.feature.chat.component.info

import com.app.common.decompose.asValue
import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.yet.talk.domain.model.chat.ChatId
import com.yet.talk.domain.model.chat.Disappearing
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.feature.chat.component.info.store.ChatInfoStore
import com.yet.talk.feature.chat.component.info.store.ChatInfoStore.Intent
import com.yet.talk.feature.chat.component.info.store.ChatInfoStoreFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

internal class DefaultChatInfoComponent(
    componentContext: ComponentContext,
    chatId: ChatId,
    private val output: (ChatInfoComponent.Output) -> Unit,
    storeFactory: ChatInfoStoreFactory,
) : ChatInfoComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore(key = "chat-info-${chatId.value}") { storeFactory.create(chatId) }

    init {
        coroutineScope().launch {
            store.labels.collect { if (it == ChatInfoStore.Label.Closed) output(ChatInfoComponent.Output.Closed) }
        }
    }

    override val model: Value<ChatInfoComponent.Model> = store.asValue().map { state ->
        ChatInfoComponent.Model(
            id = state.id,
            members = state.members,
            name = state.chat?.name,
            isGroup = state.id.isGroup,
            disappearing = Disappearing.of(state.chat?.disappearing),
            isMuted = state.chat?.isMuted == true,
            isBlocked = state.id.members.any { it in state.blocked },
            error = state.error,
        )
    }

    override fun onDisappearingChanged(disappearing: Disappearing) = store.accept(Intent.SetDisappearing(disappearing))

    override fun onMutedChanged(muted: Boolean) = store.accept(Intent.SetMuted(muted))

    override fun onBlockedChanged(blocked: Boolean) = store.accept(Intent.SetBlocked(blocked))

    override fun onLeave() = store.accept(Intent.Leave)

    override fun onOpenMember(id: UserId) = output(ChatInfoComponent.Output.OpenProfile(id))

    override fun onBack() = output(ChatInfoComponent.Output.Back)

    override fun onErrorShown() = store.accept(Intent.DismissError)
}

@Inject
internal class DefaultChatInfoComponentFactory(private val storeFactory: ChatInfoStoreFactory) : ChatInfoComponent.Factory {
    override fun invoke(componentContext: ComponentContext, chatId: ChatId, output: (ChatInfoComponent.Output) -> Unit): ChatInfoComponent =
        DefaultChatInfoComponent(componentContext, chatId, output, storeFactory)
}
