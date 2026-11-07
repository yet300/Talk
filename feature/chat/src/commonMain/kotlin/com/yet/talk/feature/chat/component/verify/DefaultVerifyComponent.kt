package com.yet.talk.feature.chat.component.verify

import com.app.common.decompose.asValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.feature.chat.component.verify.store.VerifyStore.Intent
import com.yet.talk.feature.chat.component.verify.store.VerifyStoreFactory
import dev.zacsweers.metro.Inject

internal class DefaultVerifyComponent(
    componentContext: ComponentContext,
    person: UserId,
    private val back: () -> Unit,
    storeFactory: VerifyStoreFactory,
) : VerifyComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore(key = "verify-${person.value}") { storeFactory.create(person) }

    override val model: Value<VerifyComponent.Model> = store.asValue().map { state ->
        VerifyComponent.Model(
            person = state.user ?: User(state.person, ""),
            code = state.code,
            verifiedAt = state.verifiedAt,
            scanMismatch = state.scanMismatch,
            error = state.error,
        )
    }

    override fun onMarkVerified() = store.accept(Intent.SetVerified(true))

    override fun onUnverify() = store.accept(Intent.SetVerified(false))

    override fun onScanned(payload: String) = store.accept(Intent.Scanned(payload))

    override fun onBack() = back()

    override fun onErrorShown() = store.accept(Intent.DismissError)
}

@Inject
internal class DefaultVerifyComponentFactory(private val storeFactory: VerifyStoreFactory) : VerifyComponent.Factory {
    override fun invoke(componentContext: ComponentContext, person: UserId, onBack: () -> Unit): VerifyComponent =
        DefaultVerifyComponent(componentContext, person, onBack, storeFactory)
}
