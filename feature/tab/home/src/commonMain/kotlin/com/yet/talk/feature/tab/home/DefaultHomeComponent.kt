package com.yet.talk.feature.tab.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

internal class DefaultHomeComponent(
    componentContext: ComponentContext,
) : HomeComponent, ComponentContext by componentContext {

    private val _model = MutableValue(HomeComponent.Model())
    override val model: Value<HomeComponent.Model> = _model

    override fun onFeedSelected(feed: HomeComponent.Feed) {
        _model.update { it.copy(feed = feed) }
    }
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultHomeComponentFactory : HomeComponent.Factory {
    override fun invoke(componentContext: ComponentContext): HomeComponent =
        DefaultHomeComponent(componentContext)
}
