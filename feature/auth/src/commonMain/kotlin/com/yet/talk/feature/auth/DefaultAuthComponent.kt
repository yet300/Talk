package com.yet.talk.feature.auth

import com.app.common.decompose.asValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.yet.talk.feature.auth.mapper.stateToModel
import com.yet.talk.feature.auth.store.AuthStore
import com.yet.talk.feature.auth.store.AuthStoreFactory
import dev.zacsweers.metro.Inject

internal class DefaultAuthComponent(
    componentContext: ComponentContext,
    storeFactory: AuthStoreFactory,
) : AuthComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create() }

    override val model: Value<AuthComponent.Model> = store.asValue().map(stateToModel)

    override fun onKeyChanged(key: String) = store.accept(AuthStore.Intent.ChangeKey(key))

    override fun onImportKey() = store.accept(AuthStore.Intent.ImportKey)

    override fun onCreateKey() = store.accept(AuthStore.Intent.CreateKey)

    override fun onErrorShown() = store.accept(AuthStore.Intent.DismissError)
}

@Inject
internal class DefaultAuthComponentFactory(private val storeFactory: AuthStoreFactory) : AuthComponent.Factory {
    override fun invoke(componentContext: ComponentContext): AuthComponent =
        DefaultAuthComponent(componentContext, storeFactory)
}
