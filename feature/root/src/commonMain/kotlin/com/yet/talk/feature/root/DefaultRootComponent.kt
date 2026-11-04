package com.yet.talk.feature.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.yet.talk.feature.tab.main.MainComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.serialization.Serializable

internal class DefaultRootComponent(
    componentContext: ComponentContext,
    private val mainFactory: MainComponent.Factory,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Main,
            handleBackButton = true,
            childFactory = ::child,
        )

    private fun child(config: Config, componentContext: ComponentContext): RootComponent.Child =
        when (config) {
            Config.Main -> RootComponent.Child.Main(mainFactory(componentContext))
        }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Main : Config
    }
}

@ContributesBinding(AppScope::class)
@Inject
class DefaultRootComponentFactory(
    private val mainFactory: MainComponent.Factory,
) : RootComponent.Factory {
    override fun invoke(componentContext: ComponentContext): RootComponent =
        DefaultRootComponent(componentContext, mainFactory)
}
