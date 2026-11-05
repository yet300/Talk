package com.yet.talk.feature.tab.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.feature.tab.activity.ActivityComponent
import com.yet.talk.feature.tab.bookmarks.BookmarksComponent
import com.yet.talk.feature.tab.home.HomeComponent
import com.yet.talk.feature.tab.search.SearchComponent
import dev.zacsweers.metro.Inject
import kotlinx.serialization.Serializable

internal class DefaultMainComponent(
    componentContext: ComponentContext,
    private val output: (MainComponent.Output) -> Unit,
    private val factories: TabFactories,
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
                MainComponent.Tab.Search -> Config.Search
                MainComponent.Tab.Activity -> Config.Activity
                MainComponent.Tab.Bookmarks -> Config.Bookmarks
                MainComponent.Tab.Profile -> Config.Profile
            },
        )
    }

    private fun child(config: Config, context: ComponentContext): MainComponent.Child =
        when (config) {
            Config.Home -> MainComponent.Child.Home(factories.home(context, ::onHomeOutput))
            Config.Search -> MainComponent.Child.Search(factories.search(context, ::onSearchOutput))
            Config.Activity -> MainComponent.Child.Activity(factories.activity(context, ::onActivityOutput))
            Config.Bookmarks -> MainComponent.Child.Bookmarks(factories.bookmarks(context, ::onBookmarksOutput))
            Config.Profile -> MainComponent.Child.Profile(factories.profile(context, null, ::onProfileOutput))
        }

    private fun onHomeOutput(event: HomeComponent.Output) = output(
        when (event) {
            is HomeComponent.Output.OpenPost -> MainComponent.Output.OpenPost(event.id)
            is HomeComponent.Output.OpenProfile -> MainComponent.Output.OpenProfile(event.id)
            is HomeComponent.Output.Reply -> MainComponent.Output.Reply(event.parent)
            is HomeComponent.Output.Quote -> MainComponent.Output.Quote(event.quoted)
            HomeComponent.Output.NewPost -> MainComponent.Output.NewPost
        },
    )

    private fun onSearchOutput(event: SearchComponent.Output) = output(
        when (event) {
            is SearchComponent.Output.OpenPost -> MainComponent.Output.OpenPost(event.id)
            is SearchComponent.Output.OpenProfile -> MainComponent.Output.OpenProfile(event.id)
            is SearchComponent.Output.Reply -> MainComponent.Output.Reply(event.parent)
            is SearchComponent.Output.Quote -> MainComponent.Output.Quote(event.quoted)
        },
    )

    private fun onActivityOutput(event: ActivityComponent.Output) = output(
        when (event) {
            is ActivityComponent.Output.OpenPost -> MainComponent.Output.OpenPost(event.id)
            is ActivityComponent.Output.OpenProfile -> MainComponent.Output.OpenProfile(event.id)
        },
    )

    private fun onBookmarksOutput(event: BookmarksComponent.Output) = output(
        when (event) {
            is BookmarksComponent.Output.OpenPost -> MainComponent.Output.OpenPost(event.id)
            is BookmarksComponent.Output.OpenProfile -> MainComponent.Output.OpenProfile(event.id)
            is BookmarksComponent.Output.Reply -> MainComponent.Output.Reply(event.parent)
            is BookmarksComponent.Output.Quote -> MainComponent.Output.Quote(event.quoted)
        },
    )

    private fun onProfileOutput(event: ProfileComponent.Output) {
        output(
            when (event) {
                // The Profile tab has no back.
                ProfileComponent.Output.Back -> return
                is ProfileComponent.Output.OpenPost -> MainComponent.Output.OpenPost(event.id)
                is ProfileComponent.Output.OpenProfile -> MainComponent.Output.OpenProfile(event.id)
                is ProfileComponent.Output.Reply -> MainComponent.Output.Reply(event.parent)
                is ProfileComponent.Output.Quote -> MainComponent.Output.Quote(event.quoted)
                ProfileComponent.Output.OpenSettings -> MainComponent.Output.OpenSettings
            },
        )
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Home : Config

        @Serializable
        data object Search : Config

        @Serializable
        data object Activity : Config

        @Serializable
        data object Bookmarks : Config

        @Serializable
        data object Profile : Config
    }
}

/** The tabs' component factories, injected together. */
@Inject
internal class TabFactories(
    val home: HomeComponent.Factory,
    val search: SearchComponent.Factory,
    val activity: ActivityComponent.Factory,
    val bookmarks: BookmarksComponent.Factory,
    val profile: ProfileComponent.Factory,
)

@Inject
internal class DefaultMainComponentFactory(private val factories: TabFactories) : MainComponent.Factory {
    override fun invoke(componentContext: ComponentContext, output: (MainComponent.Output) -> Unit): MainComponent =
        DefaultMainComponent(componentContext, output, factories)
}
