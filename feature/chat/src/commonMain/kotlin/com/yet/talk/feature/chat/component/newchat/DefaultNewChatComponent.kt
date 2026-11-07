package com.yet.talk.feature.chat.component.newchat

import com.app.common.decompose.asValue
import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.yet.talk.domain.model.user.User
import com.yet.talk.feature.chat.component.newchat.store.NewChatStore
import com.yet.talk.feature.chat.component.newchat.store.NewChatStoreFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

internal class DefaultNewChatComponent(
    componentContext: ComponentContext,
    private val output: (NewChatComponent.Output) -> Unit,
    storeFactory: NewChatStoreFactory,
) : NewChatComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create() }

    init {
        coroutineScope().launch {
            store.labels.collect { if (it is NewChatStore.Label.Open) output(NewChatComponent.Output.Open(it.id)) }
        }
    }

    override val model: Value<NewChatComponent.Model> = store.asValue().map {
        NewChatComponent.Model(it.query, it.people, it.isSearching, it.checking, it.cannotReceive, it.error)
    }

    override fun onQueryChanged(query: String) = store.accept(NewChatStore.Intent.ChangeQuery(query))

    override fun onPick(user: User) = store.accept(NewChatStore.Intent.Pick(user))

    override fun onBack() = output(NewChatComponent.Output.Back)

    override fun onErrorShown() = store.accept(NewChatStore.Intent.DismissError)
}

@Inject
internal class DefaultNewChatComponentFactory(private val storeFactory: NewChatStoreFactory) : NewChatComponent.Factory {
    override fun invoke(componentContext: ComponentContext, output: (NewChatComponent.Output) -> Unit): NewChatComponent =
        DefaultNewChatComponent(componentContext, output, storeFactory)
}
