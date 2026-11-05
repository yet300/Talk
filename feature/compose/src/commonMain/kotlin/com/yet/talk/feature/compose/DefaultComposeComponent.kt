package com.yet.talk.feature.compose

import com.app.common.decompose.asValue
import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.feature.compose.mapper.stateToModel
import com.yet.talk.feature.compose.store.ComposeStore
import com.yet.talk.feature.compose.store.ComposeStoreFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

internal class DefaultComposeComponent(
    componentContext: ComponentContext,
    mode: ComposeMode,
    private val output: (ComposeComponent.Output) -> Unit,
    storeFactory: ComposeStoreFactory,
) : ComposeComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore {
        storeFactory.create(mode, draft = stateKeeper.consume(DRAFT_KEY, Draft.serializer())?.text)
    }

    init {
        stateKeeper.register(DRAFT_KEY, Draft.serializer()) { Draft(store.state.text) }
        coroutineScope().launch {
            store.labels.collect { label ->
                when (label) {
                    is ComposeStore.Label.Published -> output(ComposeComponent.Output.Published(label.id))
                }
            }
        }
    }

    override val model: Value<ComposeComponent.Model> = store.asValue().map(stateToModel)

    override fun onTextChanged(text: String) = store.accept(ComposeStore.Intent.ChangeText(text))

    override fun onViewChanged(view: ComposeComponent.View) = store.accept(ComposeStore.Intent.ChangeView(view))

    override fun onAttachImage(file: ImageFile) = store.accept(ComposeStore.Intent.AttachImage(file))

    override fun onRemoveImage(url: String) = store.accept(ComposeStore.Intent.RemoveImage(url))

    override fun onPublish() = store.accept(ComposeStore.Intent.Publish)

    override fun onClose() = output(ComposeComponent.Output.Closed)

    override fun onErrorShown() = store.accept(ComposeStore.Intent.DismissError)

    @Serializable
    private data class Draft(val text: String)

    private companion object {
        const val DRAFT_KEY = "compose_draft"
    }
}

@Inject
internal class DefaultComposeComponentFactory(private val storeFactory: ComposeStoreFactory) : ComposeComponent.Factory {
    override fun invoke(
        componentContext: ComponentContext,
        mode: ComposeMode,
        output: (ComposeComponent.Output) -> Unit,
    ): ComposeComponent = DefaultComposeComponent(componentContext, mode, output, storeFactory)
}
