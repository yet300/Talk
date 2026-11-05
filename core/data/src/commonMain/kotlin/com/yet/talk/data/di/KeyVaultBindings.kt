package com.yet.talk.data.di

import com.yet.talk.data.session.KSafeKeyVault
import com.yet.talk.data.session.KeyVault
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphPrivate

/** The secret key's storage (KSafe, provided by the platform graph). Separate so tests can exclude it. */
@ContributesTo(AppScope::class)
@BindingContainer
abstract class KeyVaultBindings {
    @GraphPrivate
    @Binds
    internal abstract val KSafeKeyVault.bindKeyVault: KeyVault
}
