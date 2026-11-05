package com.yet.talk.feature.profile.di

import com.yet.talk.feature.profile.DefaultProfileComponentFactory
import com.yet.talk.feature.profile.ProfileComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class ProfileBindings {
    @Binds
    internal abstract val DefaultProfileComponentFactory.bindProfileComponentFactory: ProfileComponent.Factory
}
