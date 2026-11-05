package com.yet.talk.data.di

import com.yet.talk.data.session.KSafeKeyVault
import com.yet.talk.data.session.KeyVault
import com.yet.talk.data.repository.PreferencesRepositoryImpl
import com.yet.talk.domain.repository.PreferencesRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphPrivate

/** What lives in KSafe (provided by the platform graph): the secret key and device preferences. Separate so tests can exclude it. */
@ContributesTo(AppScope::class)
@BindingContainer
abstract class KeyVaultBindings {
    @GraphPrivate
    @Binds
    internal abstract val KSafeKeyVault.bindKeyVault: KeyVault

    @Binds
    internal abstract val PreferencesRepositoryImpl.bindPreferencesRepository: PreferencesRepository
}
