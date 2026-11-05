package com.yet.talk.feature.post.component.revisions

import com.app.common.decompose.asValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.feature.post.component.revisions.mapper.stateToModel
import com.yet.talk.feature.post.component.revisions.store.RevisionsStore
import com.yet.talk.feature.post.component.revisions.store.RevisionsStoreFactory
import dev.zacsweers.metro.Inject

internal class DefaultRevisionsComponent(
    componentContext: ComponentContext,
    id: PostId,
    private val onBack: () -> Unit,
    storeFactory: RevisionsStoreFactory,
) : RevisionsComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create(id) }

    override val model: Value<RevisionsComponent.Model> = store.asValue().map(stateToModel)

    override fun onSelect(number: Int) = store.accept(RevisionsStore.Intent.Select(number))

    override fun onRefresh() = store.accept(RevisionsStore.Intent.Refresh)

    override fun onBack() = onBack.invoke()

    override fun onErrorShown() = store.accept(RevisionsStore.Intent.DismissError)
}

@Inject
internal class DefaultRevisionsComponentFactory(private val storeFactory: RevisionsStoreFactory) : RevisionsComponent.Factory {
    override fun invoke(componentContext: ComponentContext, id: PostId, onBack: () -> Unit): RevisionsComponent =
        DefaultRevisionsComponent(componentContext, id, onBack, storeFactory)
}
