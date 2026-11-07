package com.yet.talk.feature.auth.di

import com.yet.talk.feature.auth.AuthComponent
import com.yet.talk.feature.auth.DefaultAuthComponentFactory
import com.yet.talk.feature.auth.lock.DefaultLockComponentFactory
import com.yet.talk.feature.auth.lock.LockComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class AuthBindings {
    @Binds
    internal abstract val DefaultAuthComponentFactory.bindAuthComponentFactory: AuthComponent.Factory

    @Binds
    internal abstract val DefaultLockComponentFactory.bindLockComponentFactory: LockComponent.Factory
}
