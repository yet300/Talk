package com.yet.talk.feature.tab.main.di

import com.yet.talk.feature.tab.main.DefaultMainComponentFactory
import com.yet.talk.feature.tab.main.MainComponent
import com.yet.talk.feature.tab.main.component.details.DefaultDetailsComponentFactory
import com.yet.talk.feature.tab.main.component.details.DetailsComponent
import com.yet.talk.feature.tab.main.component.tabs.DefaultTabsComponentFactory
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class MainBindings {
    @Binds
    internal abstract val DefaultMainComponentFactory.bindMainComponentFactory: MainComponent.Factory

    @Binds
    internal abstract val DefaultTabsComponentFactory.bindTabsComponentFactory: TabsComponent.Factory

    @Binds
    internal abstract val DefaultDetailsComponentFactory.bindDetailsComponentFactory: DetailsComponent.Factory
}
