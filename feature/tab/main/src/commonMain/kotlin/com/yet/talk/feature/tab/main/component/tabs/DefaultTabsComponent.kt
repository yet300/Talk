package com.yet.talk.feature.tab.main.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.childStackWebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.app.common.decompose.coroutineScope
import kotlinx.coroutines.launch
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.feature.tab.activity.ActivityComponent
import com.yet.talk.feature.tab.bookmarks.BookmarksComponent
import com.yet.talk.feature.tab.home.HomeComponent
import com.yet.talk.feature.tab.search.SearchComponent
import dev.zacsweers.metro.Inject
import kotlinx.serialization.Serializable

internal class DefaultTabsComponent(
    componentContext: ComponentContext,
    private val output: (TabsComponent.Output) -> Unit,
    private val factories: TabFactories,
    meSource: MeSource,
) : TabsComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    private val meValue = MutableValue(TabsComponent.Me())
    override val me: Value<TabsComponent.Me> = meValue

    init {
        coroutineScope().launch { meSource.observe().collect { meValue.value = it } }
    }

    private val configStack: Value<ChildStack<Config, TabsComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Home,
            handleBackButton = true,
            childFactory = ::child,
        )

    override val stack: Value<ChildStack<*, TabsComponent.Child>> = configStack

    override val webNavigation: WebNavigation<*> =
        childStackWebNavigation(
            navigator = navigation,
            stack = configStack,
            serializer = Config.serializer(),
            enableHistory = false,
            pathMapper = { it.configuration.path },
        )

    override fun onTabClicked(tab: TabsComponent.Tab) {
        navigation.bringToFront(
            when (tab) {
                TabsComponent.Tab.Home -> Config.Home
                TabsComponent.Tab.Search -> Config.Search
                TabsComponent.Tab.Activity -> Config.Activity
                TabsComponent.Tab.Bookmarks -> Config.Bookmarks
                TabsComponent.Tab.Profile -> Config.Profile
            },
        )
    }

    private fun child(config: Config, context: ComponentContext): TabsComponent.Child =
        when (config) {
            Config.Home -> TabsComponent.Child.Home(factories.home(context, ::onHomeOutput))
            Config.Search -> TabsComponent.Child.Search(factories.search(context, ::onSearchOutput))
            Config.Activity -> TabsComponent.Child.Activity(factories.activity(context, ::onActivityOutput))
            Config.Bookmarks -> TabsComponent.Child.Bookmarks(factories.bookmarks(context, ::onBookmarksOutput))
            Config.Profile -> TabsComponent.Child.Profile(factories.profile(context, null, ::onProfileOutput))
        }

    private fun onHomeOutput(event: HomeComponent.Output) = output(
        when (event) {
            is HomeComponent.Output.OpenPost -> TabsComponent.Output.OpenPost(event.id)
            is HomeComponent.Output.OpenProfile -> TabsComponent.Output.OpenProfile(event.id)
            is HomeComponent.Output.Reply -> TabsComponent.Output.Reply(event.parent)
            is HomeComponent.Output.Quote -> TabsComponent.Output.Quote(event.quoted)
            HomeComponent.Output.NewPost -> TabsComponent.Output.NewPost
        },
    )

    private fun onSearchOutput(event: SearchComponent.Output) = output(
        when (event) {
            is SearchComponent.Output.OpenPost -> TabsComponent.Output.OpenPost(event.id)
            is SearchComponent.Output.OpenProfile -> TabsComponent.Output.OpenProfile(event.id)
            is SearchComponent.Output.Reply -> TabsComponent.Output.Reply(event.parent)
            is SearchComponent.Output.Quote -> TabsComponent.Output.Quote(event.quoted)
        },
    )

    private fun onActivityOutput(event: ActivityComponent.Output) = output(
        when (event) {
            is ActivityComponent.Output.OpenPost -> TabsComponent.Output.OpenPost(event.id)
            is ActivityComponent.Output.OpenProfile -> TabsComponent.Output.OpenProfile(event.id)
        },
    )

    private fun onBookmarksOutput(event: BookmarksComponent.Output) {
        output(
            when (event) {
                is BookmarksComponent.Output.OpenPost -> TabsComponent.Output.OpenPost(event.id)
                is BookmarksComponent.Output.OpenProfile -> TabsComponent.Output.OpenProfile(event.id)
                is BookmarksComponent.Output.Reply -> TabsComponent.Output.Reply(event.parent)
                is BookmarksComponent.Output.Quote -> TabsComponent.Output.Quote(event.quoted)
                BookmarksComponent.Output.BrowseHome -> return onTabClicked(TabsComponent.Tab.Home)
            },
        )
    }

    private fun onProfileOutput(event: ProfileComponent.Output) {
        output(
            when (event) {
                // The Profile tab has no back.
                ProfileComponent.Output.Back -> return
                is ProfileComponent.Output.OpenPost -> TabsComponent.Output.OpenPost(event.id)
                is ProfileComponent.Output.OpenProfile -> TabsComponent.Output.OpenProfile(event.id)
                is ProfileComponent.Output.Reply -> TabsComponent.Output.Reply(event.parent)
                is ProfileComponent.Output.Quote -> TabsComponent.Output.Quote(event.quoted)
                ProfileComponent.Output.OpenSettings -> TabsComponent.Output.OpenSettings
            },
        )
    }

    @Serializable
    private sealed class Config(val path: String?) {
        @Serializable
        data object Home : Config(null)

        @Serializable
        data object Search : Config("search")

        @Serializable
        data object Activity : Config("activity")

        @Serializable
        data object Bookmarks : Config("bookmarks")

        @Serializable
        data object Profile : Config("me")
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
internal class DefaultTabsComponentFactory(
    private val factories: TabFactories,
    private val meSource: MeSource,
) : TabsComponent.Factory {
    override fun invoke(componentContext: ComponentContext, output: (TabsComponent.Output) -> Unit): TabsComponent =
        DefaultTabsComponent(componentContext, output, factories, meSource)
}
