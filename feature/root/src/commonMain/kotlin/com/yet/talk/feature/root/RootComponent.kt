package com.yet.talk.feature.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yet.talk.feature.auth.AuthComponent
import com.yet.talk.feature.compose.ComposeComponent
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.feature.post.component.revisions.RevisionsComponent
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.feature.tab.main.MainComponent

/**
 * The app's root: sign-in when there is no account, otherwise the tabs with full-screen destinations
 * (post, revisions, editor, someone's profile, settings) stacked above them.
 */
interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    /** Pops the top destination (the platform's back gesture is handled by the stack itself). */
    fun onBack()

    sealed class Child {
        /** Until the saved key has been read. */
        data object Splash : Child()

        class Auth(val component: AuthComponent) : Child()

        class Main(val component: MainComponent) : Child()

        class Post(val component: PostComponent) : Child()

        class Revisions(val component: RevisionsComponent) : Child()

        class Compose(val component: ComposeComponent) : Child()

        class Profile(val component: ProfileComponent) : Child()

        class Settings(val component: SettingsComponent) : Child()
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext): RootComponent
    }
}
