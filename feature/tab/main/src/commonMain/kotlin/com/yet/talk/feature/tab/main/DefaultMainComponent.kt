package com.yet.talk.feature.tab.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.yet.talk.feature.tab.home.HomeComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.serialization.Serializable

internal class DefaultMainComponent(
    componentContext: ComponentContext,
    private val homeFactory: HomeComponent.Factory,
) : MainComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, MainComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Home,
            handleBackButton = true,
            childFactory = ::child,
        )

    override fun onTabClicked(tab: MainComponent.Tab) {
        navigation.bringToFront(
            when (tab) {
                MainComponent.Tab.Home -> Config.Home
            }
        )
    }

    private fun child(config: Config, componentContext: ComponentContext): MainComponent.Child =
        when (config) {
            Config.Home -> MainComponent.Child.Home(homeFactory(componentContext))
        }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Home : Config
    }
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultMainComponentFactory(
    private val homeFactory: HomeComponent.Factory,
) : MainComponent.Factory {
    override fun invoke(componentContext: ComponentContext): MainComponent =
        DefaultMainComponent(componentContext, homeFactory)
}
