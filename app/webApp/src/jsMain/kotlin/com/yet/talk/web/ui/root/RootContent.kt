package com.yet.talk.web.ui.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import com.yet.talk.feature.editprofile.EditProfileComponent
import com.yet.talk.feature.root.RootComponent
import com.yet.talk.feature.tab.main.MainComponent
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.rememberIsPhone
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.screens.editprofile.EditProfileContent
import com.yet.talk.web.ui.components.post.LocalPostCards
import com.yet.talk.web.ui.components.moderation.LocalModeration
import com.yet.talk.web.ui.components.moderation.LocalModerationActions
import com.yet.talk.web.ui.root.component.RailActions
import com.yet.talk.web.ui.root.component.RailSelection
import com.yet.talk.web.ui.root.component.ShellWidth
import com.yet.talk.web.ui.root.component.WebShell
import com.yet.talk.web.ui.components.LocalNetwork
import com.yet.talk.web.ui.screens.auth.AuthContent
import com.yet.talk.web.ui.screens.lock.LockContent
import com.yet.talk.web.ui.screens.compose.ComposeContent
import com.yet.talk.web.ui.screens.main.MainContent
import com.yet.talk.web.ui.screens.revisions.RevisionsContent
import com.yet.talk.web.ui.screens.revisions.RevisionsSide
import com.yet.talk.web.ui.screens.settings.SettingsContent
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

/**
 * The root stack on the web. Only the active destination is drawn (the browser owns back and forward).
 * Editor, settings and revisions keep the rail, which reads the signed-in user from Main below them.
 */
@Composable
fun RootContent(component: RootComponent) {
    val stack by component.stack.subscribeAsState()
    val main = stack.items.firstNotNullOfOrNull { (it.instance as? RootComponent.Child.Main)?.component }
    val railActions = RailActions(
        onTab = component::onTabSelected,
        onCompose = component::onNewPost,
        onSettings = component::onOpenSettings,
    )
    val child = stack.active.instance
    val moderation by component.moderation.model.subscribeAsState()
    val network by component.network.subscribeAsState()
    CompositionLocalProvider(
        LocalNetwork provides network,
        LocalPostCards provides component.postCards,
        LocalModeration provides moderation,
        LocalModerationActions provides component.moderation,
    ) {
        if (child is RootComponent.Child.EditProfile) {
            EditProfile(child.component, below = stack.backStack.lastOrNull()?.instance, main, railActions)
        } else {
            Destination(child, main, railActions)
        }
    }
}

@Composable
private fun Destination(child: RootComponent.Child, main: MainComponent?, railActions: RailActions) {
    when (child) {
        RootComponent.Child.Splash -> Splash()
        is RootComponent.Child.Auth -> AuthContent(child.component)
        is RootComponent.Child.Lock -> LockContent(child.component)
        is RootComponent.Child.Main -> MainContent(child.component, railActions)
        is RootComponent.Child.Compose -> Shell(main, RailSelection.None, railActions, ShellWidth.Wide) {
            ComposeContent(child.component)
        }
        is RootComponent.Child.Settings -> Shell(main, RailSelection.Settings, railActions) {
            SettingsContent(child.component)
        }
        is RootComponent.Child.Revisions -> Shell(main, RailSelection.None, railActions, right = { RevisionsSide(child.component) }) {
            RevisionsContent(child.component)
        }
        // Full screen on a phone (no navigation capsule), as in EditProfile · Mobile.
        is RootComponent.Child.EditProfile -> Shell(main, RailSelection.None, railActions, ShellWidth.Wide) { EditProfileContent(child.component) }
    }
}

/** `EditProfile · Web`: a modal over the screen it was opened from; `EditProfile · Mobile`: the whole screen. */
@Composable
private fun EditProfile(component: EditProfileComponent, below: RootComponent.Child?, main: MainComponent?, railActions: RailActions) {
    if (rememberIsPhone() || below == null) {
        Destination(RootComponent.Child.EditProfile(component), main, railActions)
        return
    }
    Destination(below, main, railActions)
    val label = stringResource(MR.strings.action_edit_profile)
    Div({ classes("x-scrim", "t-layer"); onClick { component.onCancel() } }) {
        Div({
            classes("x-modal", "t-edit-modal")
            attr("role", "dialog")
            attr("aria-modal", "true")
            attr("aria-label", label)
            onClick { it.stopPropagation() }
        }) { EditProfileContent(component) }
    }
}

@Composable
private fun Shell(
    main: MainComponent?,
    selection: RailSelection,
    actions: RailActions,
    width: ShellWidth = ShellWidth.Column,
    right: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val panels = main?.panels?.subscribeAsState()?.value
    val me = panels?.main?.instance?.me?.subscribeAsState()?.value ?: TabsComponent.Me()
    WebShell(selection, me, actions, width, right, main = content)
}

/** Until the saved key has been read: the wordmark on the page background. */
@Composable
private fun Splash() {
    Div({ classes("talk", "t-auth-page") }) {
        Div({ classes("t-wordmark") }) { Text(stringResource(MR.strings.app_name)) }
    }
}
