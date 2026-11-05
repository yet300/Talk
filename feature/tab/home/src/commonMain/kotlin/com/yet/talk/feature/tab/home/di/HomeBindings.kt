package com.yet.talk.feature.tab.home.di

import com.yet.talk.feature.tab.home.HomeComponent
import com.yet.talk.feature.tab.home.DefaultHomeComponentFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class HomeBindings {
    @Binds
    internal abstract val DefaultHomeComponentFactory.bindHomeComponentFactory: HomeComponent.Factory
}
