package com.yet.talk.feature.settings.di

import com.yet.talk.feature.settings.DefaultSettingsComponentFactory
import com.yet.talk.feature.settings.SettingsComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class SettingsBindings {
    @Binds
    internal abstract val DefaultSettingsComponentFactory.bindSettingsComponentFactory: SettingsComponent.Factory
}
