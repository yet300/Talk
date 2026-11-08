package com.yet.talk.feature.root

import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStackWebNavigation
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popWhile
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.app.common.decompose.asValue
import com.yet.talk.domain.model.settings.Privacy
import com.yet.talk.domain.model.settings.ThemeMode
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.feature.auth.AuthComponent
import com.yet.talk.feature.compose.ComposeComponent
import com.yet.talk.feature.compose.ComposeMode
import com.yet.talk.feature.post.component.revisions.RevisionsComponent
import com.yet.talk.feature.root.store.RootStoreFactory
import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.feature.post.component.cards.PostCardsComponent
import com.yet.talk.feature.root.moderation.ModerationComponent
import com.yet.talk.feature.root.store.RootStore
import com.yet.talk.feature.auth.lock.LockComponent
import com.yet.talk.feature.editprofile.EditProfileComponent
import com.yet.talk.feature.tab.main.MainComponent
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import dev.zacsweers.metro.Inject
import com.arkivanov.decompose.value.MutableValue
import com.yet.talk.domain.model.network.NetworkStatus
import com.yet.talk.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

internal class DefaultRootComponent(
    componentContext: ComponentContext,
    storeFactory: RootStoreFactory,
    private val factories: DestinationFactories,
    deepLink: String?,
    cards: PostCardsComponent,
    override val moderation: ModerationComponent,
    networkStatus: Flow<NetworkStatus>,
) : RootComponent, ComponentContext by componentContext {

    private val networkValue = MutableValue(NetworkStatus())
    override val network: Value<NetworkStatus> = networkValue

    private val store = instanceKeeper.getStore { storeFactory.create() }
    private val navigation = StackNavigation<Config>()

    /** Opened on the first sign-in of this run; consumed once. */
    private var pendingDeepLink: DeepLink? = deepLink?.let(DeepLink::parse)

    private val configStack: Value<ChildStack<Config, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Splash,
            handleBackButton = true,
            childFactory = ::child,
        )

    override val stack: Value<ChildStack<*, RootComponent.Child>> = configStack

    override val theme: Value<ThemeMode> = store.asValue().map { it.theme }

    override val privacy: Value<Privacy> = store.asValue().map { it.privacy }

    override val postCards: PostCardsComponent = object : PostCardsComponent by cards {
        override fun openVideo(id: String) {
            navigation.popWhile { it != Config.Main }
            main?.openVideo(id)
        }
    }

    override val webNavigation: WebNavigation<*> =
        childStackWebNavigation(
            navigator = navigation,
            stack = configStack,
            serializer = Config.serializer(),
            pathMapper = { it.configuration.path },
            childSelector = { (it.instance as? RootComponent.Child.Main)?.component },
        )

    init {
        coroutineScope().launch { networkStatus.collect { networkValue.value = it } }
        coroutineScope().launch {
            store.states.map { it.access }.filterNotNull().distinctUntilChanged().collect { access ->
                val beforeMain = configStack.value.active.configuration.let { it == Config.Splash || it == Config.Auth || it == Config.Lock }
                when {
                    access == RootStore.Access.SignedOut -> navigation.replaceAll(Config.Auth)
                    // Locked: nothing of the account shows until the passcode opens it.
                    access == RootStore.Access.Locked -> navigation.replaceAll(Config.Lock)
                    beforeMain -> navigation.replaceAll(Config.Main, *pendingDeepLink?.above.orEmpty().toTypedArray())
                }
            }
        }
    }

    override fun onBack() = navigation.pop()

    override fun onTabSelected(tab: TabsComponent.Tab) {
        navigation.popWhile { it != Config.Main }
        main?.onTabSelected(tab)
    }

    override fun onNewPost() = navigation.pushNew(Config.Compose.new())

    override fun onOpenSettings() = navigation.pushToFront(Config.Settings)

    private fun child(config: Config, context: ComponentContext): RootComponent.Child =
        when (config) {
            Config.Splash -> RootComponent.Child.Splash
            Config.Auth -> RootComponent.Child.Auth(factories.auth(context))
            Config.Lock -> RootComponent.Child.Lock(factories.lock(context))
            Config.Main -> {
                val link = pendingDeepLink
                pendingDeepLink = null
                val main = factories.main(context, link?.main, ::onMainOutput)
                link?.tab?.let(main::onTabSelected)
                RootComponent.Child.Main(main)
            }
            is Config.Revisions -> RootComponent.Child.Revisions(factories.revisions(context, PostId(config.id), navigation::pop))
            is Config.Compose -> RootComponent.Child.Compose(factories.compose(context, config.toMode()) { onComposeOutput(config, it) })
            Config.Settings -> RootComponent.Child.Settings(factories.settings(context, ::onSettingsOutput))
            Config.EditProfile -> RootComponent.Child.EditProfile(factories.editProfile(context) { navigation.pop() })
        }

    private fun onMainOutput(event: MainComponent.Output) = when (event) {
        is MainComponent.Output.Reply -> navigation.pushNew(Config.Compose.reply(event.parent))
        is MainComponent.Output.Quote -> navigation.pushNew(Config.Compose.quote(event.quoted))
        is MainComponent.Output.Edit -> navigation.pushNew(Config.Compose.edit(event.id))
        is MainComponent.Output.OpenRevisions -> navigation.pushNew(Config.Revisions(event.id.value))
        MainComponent.Output.NewPost -> navigation.pushNew(Config.Compose.new())
        MainComponent.Output.OpenSettings -> navigation.pushNew(Config.Settings)
        MainComponent.Output.EditProfile -> navigation.pushNew(Config.EditProfile)
        is MainComponent.Output.Share -> navigation.pushNew(Config.Compose.share(event.text))
    }

    private fun onSettingsOutput(event: SettingsComponent.Output) = when (event) {
        SettingsComponent.Output.Back -> navigation.pop()
        SettingsComponent.Output.EditProfile -> navigation.pushNew(Config.EditProfile)
    }

    /** KarpathyTalk shows a new post or quote after publishing; a reply or an edit returns to where it came from. */
    private fun onComposeOutput(config: Config.Compose, event: ComposeComponent.Output) {
        when (event) {
            ComposeComponent.Output.Closed -> navigation.pop()
            is ComposeComponent.Output.Published -> when (config.kind) {
                Config.Compose.Kind.New, Config.Compose.Kind.Share, Config.Compose.Kind.Quote -> navigation.pop { main?.openPost(event.id) }
                Config.Compose.Kind.Reply, Config.Compose.Kind.Edit -> navigation.pop()
            }
        }
    }

    private val main: MainComponent?
        get() = configStack.value.items.firstNotNullOfOrNull { (it.instance as? RootComponent.Child.Main)?.component }

    /**
     * A shared link: `/post/<id>`, `/user/<id>` and `/video/<id>` open in Main's details panel; `/post/<id>/revisions` also
     * opens the revisions above it, `/settings` the settings; a tab's path (`/search`, `/activity`,
     * `/bookmarks`, `/me`) selects that tab. Anything else opens Main as it is.
     */
    private class DeepLink(val main: MainComponent.Link?, val above: List<Config>, val tab: TabsComponent.Tab? = null) {
        companion object {
            fun parse(link: String): DeepLink? {
                val path = if ("://" in link) link.substringAfter("://").substringAfter('/', "") else link
                val segments = path.substringBefore('?').substringBefore('#').split('/').filter(String::isNotEmpty)
                return when {
                    segments.size == 2 && segments[0] == "post" -> DeepLink(MainComponent.Link.Post(PostId(segments[1])), emptyList())
                    segments.size == 3 && segments[0] == "post" && segments[2] == "revisions" ->
                        DeepLink(MainComponent.Link.Post(PostId(segments[1])), listOf(Config.Revisions(segments[1])))
                    segments.size == 2 && segments[0] == "user" -> DeepLink(MainComponent.Link.Profile(UserId(segments[1])), emptyList())
                    segments.size == 2 && segments[0] == "video" -> DeepLink(MainComponent.Link.Video(segments[1]), emptyList())
                    segments == listOf("settings") -> DeepLink(null, listOf(Config.Settings))
                    segments.size == 1 -> TabsComponent.Tab.entries.firstOrNull { it.path == segments[0] }
                        ?.let { DeepLink(null, emptyList(), tab = it) }
                    else -> null
                }
            }
        }
    }

    @Serializable
    private sealed interface Config {
        /** This destination's part of the web URL; null leaves the URL to the child (Main's tabs) or at `/`. */
        val path: String? get() = null

        @Serializable
        data object Splash : Config

        @Serializable
        data object Auth : Config {
            override val path get() = "signin"
        }

        @Serializable
        data object Lock : Config

        @Serializable
        data object Main : Config

        @Serializable
        data class Revisions(val id: String) : Config {
            override val path get() = "post/$id/revisions"
        }

        @Serializable
        data object Settings : Config {
            override val path get() = "settings"
        }

        /** A modal on the web, full screen on the phone; saved or cancelled, it closes. */
        @Serializable
        data object EditProfile : Config {
            override val path get() = "settings/profile"
        }

        @Serializable
        data class Compose(val kind: Kind, val target: String?) : Config {
            enum class Kind { New, Share, Reply, Quote, Edit }

            override val path get() = "compose"

            fun toMode(): ComposeMode = when (kind) {
                Kind.New -> ComposeMode.New
                Kind.Share -> ComposeMode.Share(requireNotNull(target))
                Kind.Reply -> ComposeMode.Reply(PostId(requireNotNull(target)))
                Kind.Quote -> ComposeMode.Quote(PostId(requireNotNull(target)))
                Kind.Edit -> ComposeMode.Edit(PostId(requireNotNull(target)))
            }

            companion object {
                fun new() = Compose(Kind.New, null)

                fun share(text: String) = Compose(Kind.Share, text)

                fun reply(parent: PostId) = Compose(Kind.Reply, parent.value)

                fun quote(quoted: PostId) = Compose(Kind.Quote, quoted.value)

                fun edit(post: PostId) = Compose(Kind.Edit, post.value)
            }
        }
    }
}

/** Every destination's component factory, injected together. */
@Inject
internal class DestinationFactories(
    val auth: AuthComponent.Factory,
    val lock: LockComponent.Factory,
    val main: MainComponent.Factory,
    val revisions: RevisionsComponent.Factory,
    val compose: ComposeComponent.Factory,
    val settings: SettingsComponent.Factory,
    val editProfile: EditProfileComponent.Factory,
)

@Inject
internal class DefaultRootComponentFactory(
    private val storeFactory: RootStoreFactory,
    private val factories: DestinationFactories,
    private val postCards: PostCardsComponent,
    private val moderation: ModerationComponent,
    private val network: NetworkRepository,
) : RootComponent.Factory {
    override fun invoke(componentContext: ComponentContext, deepLink: String?): RootComponent =
        DefaultRootComponent(componentContext, storeFactory, factories, deepLink, postCards, moderation, network.status)
}
