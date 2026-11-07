package com.yet.talk.feature.root.di

import com.yet.talk.feature.root.RootComponent
import com.yet.talk.feature.root.DefaultRootComponentFactory
import com.yet.talk.feature.root.moderation.DefaultModerationComponent
import com.yet.talk.feature.root.moderation.ModerationComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class RootBindings {
    @Binds
    internal abstract val DefaultRootComponentFactory.bindRootComponentFactory: RootComponent.Factory

    @Binds
    internal abstract val DefaultModerationComponent.bindModerationComponent: ModerationComponent
}
