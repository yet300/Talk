package com.yet.talk.feature.tab.main.component.details

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.feature.tab.main.MainComponent

/**
 * The details panel: a post, and whatever is opened from it (an author, another post), as a stack.
 * Covers the tabs in single-pane layouts and sits next to them in two-pane ones.
 */
interface DetailsComponent : WebNavigationOwner {
    val stack: Value<ChildStack<*, Child>>

    /** What the panel was opened with; the list next to it highlights this post. */
    val link: MainComponent.Link

    /** Pops the top page, or closes the panel from its first page. */
    fun onBack()

    /** Opens a page on top of the current one. */
    fun open(link: MainComponent.Link)

    sealed class Child {
        class Post(val component: PostComponent) : Child()

        class Profile(val component: ProfileComponent) : Child()
    }

    sealed interface Output {
        /** Back from the first page. */
        data object Closed : Output

        data class Reply(val parent: PostId) : Output

        data class Quote(val quoted: PostId) : Output

        data class Edit(val id: PostId) : Output

        data class OpenRevisions(val id: PostId) : Output

        data object OpenSettings : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, link: MainComponent.Link, output: (Output) -> Unit): DetailsComponent
    }
}
