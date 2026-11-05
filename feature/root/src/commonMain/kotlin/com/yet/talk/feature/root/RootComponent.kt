package com.yet.talk.feature.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.yet.talk.feature.auth.AuthComponent
import com.yet.talk.feature.compose.ComposeComponent
import com.yet.talk.feature.post.component.revisions.RevisionsComponent
import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.feature.tab.main.MainComponent

/**
 * The app's root: sign-in when there is no account, otherwise [MainComponent] (tabs with posts and profiles
 * in list-detail panels) with full-screen destinations (editor, revisions, settings) stacked above it.
 *
 * [backHandler] drives predictive back on Android and the swipe back on iOS; [webNavigation] binds the
 * stack to the browser history (`/post/<id>`, `/user/<id>`, `/settings`, tabs below `Main`).
 */
interface RootComponent : BackHandlerOwner, WebNavigationOwner {
    val stack: Value<ChildStack<*, Child>>

    /** Pops the top destination (the platform's back gesture is handled by the stack itself). */
    fun onBack()

    sealed class Child {
        /** Until the saved key has been read. */
        data object Splash : Child()

        class Auth(val component: AuthComponent) : Child()

        class Main(val component: MainComponent) : Child()

        class Revisions(val component: RevisionsComponent) : Child()

        class Compose(val component: ComposeComponent) : Child()

        class Settings(val component: SettingsComponent) : Child()
    }

    fun interface Factory {
        /** [deepLink] is a URL or path to open once signed in (web: the address the page was opened at). */
        operator fun invoke(componentContext: ComponentContext, deepLink: String?): RootComponent
    }
}
