package com.yet.talk.feature.root

import com.app.common.decompose.coroutineScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.feature.auth.AuthComponent
import com.yet.talk.feature.compose.ComposeComponent
import com.yet.talk.feature.compose.ComposeMode
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.feature.post.component.revisions.RevisionsComponent
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.feature.root.store.RootStoreFactory
import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.feature.tab.main.MainComponent
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

internal class DefaultRootComponent(
    componentContext: ComponentContext,
    storeFactory: RootStoreFactory,
    private val factories: DestinationFactories,
) : RootComponent, ComponentContext by componentContext {

    private val store = instanceKeeper.getStore { storeFactory.create() }
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Splash,
            handleBackButton = true,
            childFactory = ::child,
        )

    init {
        coroutineScope().launch {
            store.states.map { it.signedIn }.filterNotNull().distinctUntilChanged().collect { signedIn ->
                val atAuthOrSplash = stack.value.active.configuration.let { it == Config.Splash || it == Config.Auth }
                when {
                    !signedIn -> navigation.replaceAll(Config.Auth)
                    atAuthOrSplash -> navigation.replaceAll(Config.Main)
                }
            }
        }
    }

    override fun onBack() = navigation.pop()

    private fun child(config: Config, context: ComponentContext): RootComponent.Child =
        when (config) {
            Config.Splash -> RootComponent.Child.Splash
            Config.Auth -> RootComponent.Child.Auth(factories.auth(context))
            Config.Main -> RootComponent.Child.Main(factories.main(context, ::onMainOutput))
            is Config.Post -> RootComponent.Child.Post(factories.post(context, PostId(config.id), ::onPostOutput))
            is Config.Revisions -> RootComponent.Child.Revisions(factories.revisions(context, PostId(config.id), navigation::pop))
            is Config.Compose -> RootComponent.Child.Compose(factories.compose(context, config.toMode()) { onComposeOutput(config, it) })
            is Config.Profile -> RootComponent.Child.Profile(factories.profile(context, UserId(config.id), ::onProfileOutput))
            Config.Settings -> RootComponent.Child.Settings(factories.settings(context, navigation::pop))
        }

    private fun onMainOutput(event: MainComponent.Output) = when (event) {
        is MainComponent.Output.OpenPost -> openPost(event.id)
        is MainComponent.Output.OpenProfile -> openProfile(event.id)
        is MainComponent.Output.Reply -> navigation.pushNew(Config.Compose.reply(event.parent))
        is MainComponent.Output.Quote -> navigation.pushNew(Config.Compose.quote(event.quoted))
        MainComponent.Output.NewPost -> navigation.pushNew(Config.Compose.new())
        MainComponent.Output.OpenSettings -> navigation.pushNew(Config.Settings)
    }

    private fun onPostOutput(event: PostComponent.Output) = when (event) {
        PostComponent.Output.Back -> navigation.pop()
        is PostComponent.Output.OpenPost -> openPost(event.id)
        is PostComponent.Output.OpenProfile -> openProfile(event.id)
        is PostComponent.Output.OpenRevisions -> navigation.pushNew(Config.Revisions(event.id.value))
        is PostComponent.Output.Reply -> navigation.pushNew(Config.Compose.reply(event.parent))
        is PostComponent.Output.Quote -> navigation.pushNew(Config.Compose.quote(event.quoted))
        is PostComponent.Output.Edit -> navigation.pushNew(Config.Compose.edit(event.id))
    }

    private fun onProfileOutput(event: ProfileComponent.Output) = when (event) {
        ProfileComponent.Output.Back -> navigation.pop()
        is ProfileComponent.Output.OpenPost -> openPost(event.id)
        is ProfileComponent.Output.OpenProfile -> openProfile(event.id)
        is ProfileComponent.Output.Reply -> navigation.pushNew(Config.Compose.reply(event.parent))
        is ProfileComponent.Output.Quote -> navigation.pushNew(Config.Compose.quote(event.quoted))
        ProfileComponent.Output.OpenSettings -> navigation.pushNew(Config.Settings)
    }

    /** KarpathyTalk shows a new post or quote after publishing; a reply or an edit returns to where it came from. */
    private fun onComposeOutput(config: Config.Compose, event: ComposeComponent.Output) {
        when (event) {
            ComposeComponent.Output.Closed -> navigation.pop()
            is ComposeComponent.Output.Published -> when (config.kind) {
                Config.Compose.Kind.New, Config.Compose.Kind.Quote -> navigation.replaceCurrent(Config.Post(event.id.value))
                Config.Compose.Kind.Reply, Config.Compose.Kind.Edit -> navigation.pop()
            }
        }
    }

    private fun openPost(id: PostId) = navigation.pushToFront(Config.Post(id.value))

    private fun openProfile(id: UserId) = navigation.pushToFront(Config.Profile(id.value))

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Splash : Config

        @Serializable
        data object Auth : Config

        @Serializable
        data object Main : Config

        @Serializable
        data class Post(val id: String) : Config

        @Serializable
        data class Revisions(val id: String) : Config

        @Serializable
        data class Profile(val id: String) : Config

        @Serializable
        data object Settings : Config

        @Serializable
        data class Compose(val kind: Kind, val target: String?) : Config {
            enum class Kind { New, Reply, Quote, Edit }

            fun toMode(): ComposeMode = when (kind) {
                Kind.New -> ComposeMode.New
                Kind.Reply -> ComposeMode.Reply(PostId(requireNotNull(target)))
                Kind.Quote -> ComposeMode.Quote(PostId(requireNotNull(target)))
                Kind.Edit -> ComposeMode.Edit(PostId(requireNotNull(target)))
            }

            companion object {
                fun new() = Compose(Kind.New, null)

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
    val main: MainComponent.Factory,
    val post: PostComponent.Factory,
    val revisions: RevisionsComponent.Factory,
    val compose: ComposeComponent.Factory,
    val profile: ProfileComponent.Factory,
    val settings: SettingsComponent.Factory,
)

@Inject
internal class DefaultRootComponentFactory(
    private val storeFactory: RootStoreFactory,
    private val factories: DestinationFactories,
) : RootComponent.Factory {
    override fun invoke(componentContext: ComponentContext): RootComponent =
        DefaultRootComponent(componentContext, storeFactory, factories)
}
