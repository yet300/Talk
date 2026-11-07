package com.yet.talk.feature.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.feature.auth.AuthComponent
import com.yet.talk.feature.compose.ComposeComponent
import com.yet.talk.feature.post.component.revisions.RevisionsComponent
import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.feature.post.component.cards.PostCardsComponent
import com.yet.talk.feature.root.moderation.ModerationComponent
import com.yet.talk.feature.editprofile.EditProfileComponent
import com.yet.talk.feature.tab.main.MainComponent
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent

/**
 * The app's root: sign-in when there is no account, otherwise [MainComponent] (tabs with posts and profiles
 * in list-detail panels) with full-screen destinations (editor, revisions, settings) stacked above it.
 *
 * [backHandler] drives predictive back on Android and the swipe back on iOS; [webNavigation] binds the
 * stack to the browser history (`/post/<id>`, `/user/<id>`, `/settings`, tabs below `Main`).
 */
interface RootComponent : BackHandlerOwner, WebNavigationOwner {
    val stack: Value<ChildStack<*, Child>>

    /** What post cards load beyond the post (link cards, quotes, Retry), on every screen. */
    val postCards: PostCardsComponent

    /** Blurring and hiding by the viewer's network, and Report. */
    val moderation: ModerationComponent

    /** The theme chosen in Settings; the platform UI applies it ([ThemeMode.System] follows the device). */
    val theme: Value<ThemeMode>

    /** Pops the top destination (the platform's back gesture is handled by the stack itself). */
    fun onBack()

    /** Navigation chrome shown above Main too (the web rail): back to Main on that tab. */
    fun onTabSelected(tab: TabsComponent.Tab)

    /** The rail's Compose button. */
    fun onNewPost()

    /** The rail's Settings item and identity row. */
    fun onOpenSettings()

    sealed class Child {
        /** Until the saved key has been read. */
        data object Splash : Child()

        class Auth(val component: AuthComponent) : Child()

        class Main(val component: MainComponent) : Child()

        class Revisions(val component: RevisionsComponent) : Child()

        class Compose(val component: ComposeComponent) : Child()

        class Settings(val component: SettingsComponent) : Child()

        class EditProfile(val component: EditProfileComponent) : Child()
    }

    fun interface Factory {
        /** [deepLink] is a URL or path to open once signed in (web: the address the page was opened at). */
        operator fun invoke(componentContext: ComponentContext, deepLink: String?): RootComponent
    }
}
