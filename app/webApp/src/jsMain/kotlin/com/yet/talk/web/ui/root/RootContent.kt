package com.yet.talk.web.ui.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.yet.talk.feature.root.RootComponent
import com.yet.talk.feature.tab.main.MainComponent
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.root.component.RailActions
import com.yet.talk.web.ui.root.component.RailSelection
import com.yet.talk.web.ui.root.component.ShellWidth
import com.yet.talk.web.ui.root.component.WebShell
import com.yet.talk.web.ui.screens.auth.AuthContent
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
    when (val child = stack.active.instance) {
        RootComponent.Child.Splash -> Splash()
        is RootComponent.Child.Auth -> AuthContent(child.component)
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
    WebShell(selection, me, actions, width, right, content)
}

/** Until the saved key has been read: the wordmark on the page background. */
@Composable
private fun Splash() {
    Div({ classes("talk", "t-auth-page") }) {
        Div({ classes("t-wordmark") }) { Text(stringResource(MR.strings.app_name)) }
    }
}
