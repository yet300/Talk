package com.yet.talk.feature.tab.main.component.details

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.childStackWebNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.feature.tab.main.MainComponent
import dev.zacsweers.metro.Inject
import kotlinx.serialization.Serializable

internal class DefaultDetailsComponent(
    componentContext: ComponentContext,
    override val link: MainComponent.Link,
    private val output: (DetailsComponent.Output) -> Unit,
    private val factories: DetailFactories,
) : DetailsComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    private val configStack: Value<ChildStack<Config, DetailsComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = link.toConfig(),
            handleBackButton = true,
            childFactory = ::child,
        )

    override val stack: Value<ChildStack<*, DetailsComponent.Child>> = configStack

    override val webNavigation: WebNavigation<*> =
        childStackWebNavigation(
            navigator = navigation,
            stack = configStack,
            serializer = Config.serializer(),
            pathMapper = { it.configuration.path },
        )

    override fun onBack() {
        if (configStack.value.backStack.isEmpty()) output(DetailsComponent.Output.Closed) else navigation.pop()
    }

    override fun open(link: MainComponent.Link) = navigation.pushToFront(link.toConfig())

    private fun child(config: Config, context: ComponentContext): DetailsComponent.Child =
        when (config) {
            is Config.Post -> DetailsComponent.Child.Post(factories.post(context, PostId(config.id), ::onPostOutput))
            is Config.Profile -> DetailsComponent.Child.Profile(factories.profile(context, UserId(config.id), ::onProfileOutput))
        }

    private fun onPostOutput(event: PostComponent.Output) = when (event) {
        PostComponent.Output.Back -> onBack()
        is PostComponent.Output.OpenPost -> open(MainComponent.Link.Post(event.id))
        is PostComponent.Output.OpenProfile -> open(MainComponent.Link.Profile(event.id))
        is PostComponent.Output.OpenRevisions -> output(DetailsComponent.Output.OpenRevisions(event.id))
        is PostComponent.Output.Reply -> output(DetailsComponent.Output.Reply(event.parent))
        is PostComponent.Output.Quote -> output(DetailsComponent.Output.Quote(event.quoted))
        is PostComponent.Output.Edit -> output(DetailsComponent.Output.Edit(event.id))
    }

    private fun onProfileOutput(event: ProfileComponent.Output) = when (event) {
        ProfileComponent.Output.Back -> onBack()
        is ProfileComponent.Output.OpenPost -> open(MainComponent.Link.Post(event.id))
        is ProfileComponent.Output.OpenProfile -> open(MainComponent.Link.Profile(event.id))
        is ProfileComponent.Output.Reply -> output(DetailsComponent.Output.Reply(event.parent))
        is ProfileComponent.Output.Quote -> output(DetailsComponent.Output.Quote(event.quoted))
        ProfileComponent.Output.OpenSettings -> output(DetailsComponent.Output.OpenSettings)
        ProfileComponent.Output.EditProfile -> output(DetailsComponent.Output.EditProfile)
        is ProfileComponent.Output.OpenRevisions -> output(DetailsComponent.Output.OpenRevisions(event.id))
    }

    @Serializable
    private sealed class Config(val path: String) {
        @Serializable
        data class Post(val id: String) : Config("post/$id")

        @Serializable
        data class Profile(val id: String) : Config("user/$id")
    }

    private fun MainComponent.Link.toConfig(): Config = when (this) {
        is MainComponent.Link.Post -> Config.Post(id.value)
        is MainComponent.Link.Profile -> Config.Profile(id.value)
    }
}

/** The pages' component factories, injected together. */
@Inject
internal class DetailFactories(
    val post: PostComponent.Factory,
    val profile: ProfileComponent.Factory,
)

@Inject
internal class DefaultDetailsComponentFactory(private val factories: DetailFactories) : DetailsComponent.Factory {
    override fun invoke(
        componentContext: ComponentContext,
        link: MainComponent.Link,
        output: (DetailsComponent.Output) -> Unit,
    ): DetailsComponent = DefaultDetailsComponent(componentContext, link, output, factories)
}
