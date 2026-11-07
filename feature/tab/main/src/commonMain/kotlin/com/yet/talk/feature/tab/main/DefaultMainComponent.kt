package com.yet.talk.feature.tab.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.arkivanov.decompose.router.panels.Panels
import com.arkivanov.decompose.router.panels.PanelsNavigation
import com.arkivanov.decompose.router.panels.childPanels
import com.arkivanov.decompose.router.panels.childPanelsWebNavigation
import com.arkivanov.decompose.router.panels.dismissDetails
import com.arkivanov.decompose.router.panels.navigate
import com.arkivanov.decompose.router.panels.setMode
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.feature.tab.main.component.details.DetailsComponent
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import dev.zacsweers.metro.Inject
import kotlinx.serialization.Serializable

internal class DefaultMainComponent(
    componentContext: ComponentContext,
    link: MainComponent.Link?,
    private val output: (MainComponent.Output) -> Unit,
    private val tabsFactory: TabsComponent.Factory,
    private val detailsFactory: DetailsComponent.Factory,
) : MainComponent, ComponentContext by componentContext {

    private val navigation = PanelsNavigation<TabsConfig, DetailsConfig, Nothing>()

    private val reading = MutableValue(false)
    override val isReading: Value<Boolean> = reading

    private val configPanels: Value<ChildPanels<TabsConfig, TabsComponent, DetailsConfig, DetailsComponent, Nothing, Nothing>> =
        childPanels(
            source = navigation,
            serializers = TabsConfig.serializer() to DetailsConfig.serializer(),
            initialPanels = { Panels(main = TabsConfig, details = link?.let(::DetailsConfig)) },
            handleBackButton = true,
            onStateChanged = { new, old -> if (new.details != old?.details || !new.mode.isTwoPane) reading.value = false },
            mainFactory = { _, context -> tabsFactory(context, ::onTabsOutput) },
            detailsFactory = { config, context -> detailsFactory(context, config.link, ::onDetailsOutput) },
        )

    override val panels: Value<ChildPanels<*, TabsComponent, *, DetailsComponent, Nothing, Nothing>> = configPanels

    private val leaveReader = BackCallback(isEnabled = false) { reading.value = false }

    override val webNavigation: WebNavigation<*> =
        childPanelsWebNavigation(
            navigator = navigation,
            panels = configPanels,
            serializers = TabsConfig.serializer() to DetailsConfig.serializer(),
            childSelector = { it.details?.instance ?: it.main.instance },
        )

    init {
        backHandler.register(leaveReader)
        reading.subscribe { leaveReader.isEnabled = it }
    }

    override fun onModeChanged(mode: ChildPanelsMode) = navigation.setMode(mode)

    override fun onShowReplies() {
        val state = configPanels.value
        if (state.mode.isTwoPane && state.details != null) reading.value = true
    }

    override fun onTabSelected(tab: TabsComponent.Tab) {
        navigation.dismissDetails()
        configPanels.value.main.instance.onTabClicked(tab)
    }

    override fun openPost(id: PostId) = showDetails(MainComponent.Link.Post(id))

    override fun openProfile(id: UserId) = showDetails(MainComponent.Link.Profile(id))

    override fun openVideo(id: String) = showDetails(MainComponent.Link.Video(id))

    private fun showDetails(link: MainComponent.Link) = navigation.navigate { it.copy(details = DetailsConfig(link)) }

    private fun onTabsOutput(event: TabsComponent.Output) = when (event) {
        is TabsComponent.Output.OpenPost -> openPost(event.id)
        is TabsComponent.Output.OpenProfile -> openProfile(event.id)
        is TabsComponent.Output.OpenVideo -> openVideo(event.id)
        is TabsComponent.Output.Reply -> output(MainComponent.Output.Reply(event.parent))
        is TabsComponent.Output.Quote -> output(MainComponent.Output.Quote(event.quoted))
        TabsComponent.Output.NewPost -> output(MainComponent.Output.NewPost)
        TabsComponent.Output.OpenSettings -> output(MainComponent.Output.OpenSettings)
        TabsComponent.Output.EditProfile -> output(MainComponent.Output.EditProfile)
        is TabsComponent.Output.OpenRevisions -> output(MainComponent.Output.OpenRevisions(event.id))
    }

    private fun onDetailsOutput(event: DetailsComponent.Output) = when (event) {
        DetailsComponent.Output.Closed -> navigation.dismissDetails()
        is DetailsComponent.Output.Reply -> output(MainComponent.Output.Reply(event.parent))
        is DetailsComponent.Output.Quote -> output(MainComponent.Output.Quote(event.quoted))
        is DetailsComponent.Output.Edit -> output(MainComponent.Output.Edit(event.id))
        is DetailsComponent.Output.OpenRevisions -> output(MainComponent.Output.OpenRevisions(event.id))
        DetailsComponent.Output.OpenSettings -> output(MainComponent.Output.OpenSettings)
        DetailsComponent.Output.EditProfile -> output(MainComponent.Output.EditProfile)
        is DetailsComponent.Output.Share -> output(MainComponent.Output.Share(event.text))
    }

    private val ChildPanelsMode.isTwoPane get() = this != ChildPanelsMode.SINGLE

    @Serializable
    private data object TabsConfig

    @Serializable
    private data class DetailsConfig(val kind: Kind, val id: String) {
        enum class Kind { Post, Profile, Video }

        constructor(link: MainComponent.Link) : this(
            kind = when (link) {
                is MainComponent.Link.Post -> Kind.Post
                is MainComponent.Link.Profile -> Kind.Profile
                is MainComponent.Link.Video -> Kind.Video
            },
            id = when (link) {
                is MainComponent.Link.Post -> link.id.value
                is MainComponent.Link.Profile -> link.id.value
                is MainComponent.Link.Video -> link.id
            },
        )

        val link: MainComponent.Link
            get() = when (kind) {
                Kind.Post -> MainComponent.Link.Post(PostId(id))
                Kind.Profile -> MainComponent.Link.Profile(UserId(id))
                Kind.Video -> MainComponent.Link.Video(id)
            }
    }
}

@Inject
internal class DefaultMainComponentFactory(
    private val tabsFactory: TabsComponent.Factory,
    private val detailsFactory: DetailsComponent.Factory,
) : MainComponent.Factory {
    override fun invoke(
        componentContext: ComponentContext,
        link: MainComponent.Link?,
        output: (MainComponent.Output) -> Unit,
    ): MainComponent = DefaultMainComponent(componentContext, link, output, tabsFactory, detailsFactory)
}
