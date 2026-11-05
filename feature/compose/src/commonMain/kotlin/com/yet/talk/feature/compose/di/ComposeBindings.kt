package com.yet.talk.feature.compose.di

import com.yet.talk.feature.compose.ComposeComponent
import com.yet.talk.feature.compose.DefaultComposeComponentFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class ComposeBindings {
    @Binds
    internal abstract val DefaultComposeComponentFactory.bindComposeComponentFactory: ComposeComponent.Factory
}
