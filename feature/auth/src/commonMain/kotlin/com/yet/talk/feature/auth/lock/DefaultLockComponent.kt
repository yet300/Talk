package com.yet.talk.feature.auth.lock

import com.app.common.decompose.asValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.yet.talk.feature.auth.lock.store.LockStore.Intent
import com.yet.talk.feature.auth.lock.store.LockStoreFactory
import dev.zacsweers.metro.Inject

internal class DefaultLockComponent(componentContext: ComponentContext, storeFactory: LockStoreFactory) :
    LockComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create() }

    override val model: Value<LockComponent.Model> = store.asValue().map { LockComponent.Model(it.passcode, it.isUnlocking, it.error) }

    override fun onPasscodeChanged(passcode: String) = store.accept(Intent.ChangePasscode(passcode))

    override fun onUnlock() = store.accept(Intent.Unlock)

    override fun onForgot() = store.accept(Intent.Forgot)

    override fun onErrorShown() = store.accept(Intent.DismissError)
}

@Inject
internal class DefaultLockComponentFactory(private val storeFactory: LockStoreFactory) : LockComponent.Factory {
    override fun invoke(componentContext: ComponentContext): LockComponent = DefaultLockComponent(componentContext, storeFactory)
}
