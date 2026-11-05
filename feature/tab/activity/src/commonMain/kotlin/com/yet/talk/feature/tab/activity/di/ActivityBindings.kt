package com.yet.talk.feature.tab.activity.di

import com.yet.talk.feature.tab.activity.ActivityComponent
import com.yet.talk.feature.tab.activity.DefaultActivityComponentFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class ActivityBindings {
    @Binds
    internal abstract val DefaultActivityComponentFactory.bindActivityComponentFactory: ActivityComponent.Factory
}
