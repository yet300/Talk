package com.yet.talk.web.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.yet.talk.feature.root.RootComponent
import com.yet.talk.resources.*
import com.yet.talk.resources.TalkTokens
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.theme.TalkColor
import com.yet.talk.web.theme.stringResource
import com.yet.talk.web.theme.textStyle
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Main
import org.jetbrains.compose.web.dom.Text

/** The root stack. Screens are placeholders until the UI phase replaces them. */
@Composable
fun RootContent(component: RootComponent) {
    val stack by component.stack.subscribeAsState()
    val title = when (stack.active.instance) {
        RootComponent.Child.Splash -> stringResource(MR.strings.app_name)
        is RootComponent.Child.Auth -> stringResource(MR.strings.auth_title)
        is RootComponent.Child.Main -> stringResource(MR.strings.tab_home)
        is RootComponent.Child.Revisions -> stringResource(MR.strings.revisions_title)
        is RootComponent.Child.Compose -> stringResource(MR.strings.compose_new)
        is RootComponent.Child.Settings -> stringResource(MR.strings.settings_title)
    }
    Main({
        style {
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            justifyContent(JustifyContent.Center)
            minHeight(100.vh)
        }
    }) {
        H1({
            style {
                textStyle(TalkTokens.Type.wordmark)
                color(TalkColor.textPrimary)
            }
        }) { Text(title) }
    }
}
