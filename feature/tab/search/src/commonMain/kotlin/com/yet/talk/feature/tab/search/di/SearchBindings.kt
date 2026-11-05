package com.yet.talk.feature.tab.search.di

import com.yet.talk.feature.tab.search.DefaultSearchComponentFactory
import com.yet.talk.feature.tab.search.SearchComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class SearchBindings {
    @Binds
    internal abstract val DefaultSearchComponentFactory.bindSearchComponentFactory: SearchComponent.Factory
}
