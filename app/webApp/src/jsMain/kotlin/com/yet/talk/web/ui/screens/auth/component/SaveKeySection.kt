package com.yet.talk.web.ui.screens.auth.component

import androidx.compose.runtime.Composable
import com.yet.talk.feature.auth.AuthComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.copyToClipboard
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

/**
 * "Save your key" (`SaveKey`), once, after Create a new key: the warning, the `nsec`, Copy, and "I saved it",
 * which signs in; it unlocks after Copy or after 3 seconds.
 */
@Composable
fun SaveKeySection(model: AuthComponent.Model, nsec: String, component: AuthComponent) {
    H1({ classes("t-auth-title") }) { Text(stringResource(MR.strings.auth_save_title)) }
    P({ classes("x-alert", "t-flush"); attr("role", "alert") }) { Text(stringResource(MR.strings.auth_save_warning)) }
    Div({ classes("t-mono-box") }) { Text(nsec) }
    TalkButton(
        text = stringResource(if (model.newKeyCopied) MR.strings.action_copied else MR.strings.action_copy),
        onClick = { copyToClipboard(nsec); component.onNewKeyCopied() },
        style = ButtonStyle.Secondary,
        attrs = { classes("t-btn-44") },
    )
    TalkButton(
        text = stringResource(MR.strings.auth_saved_it),
        onClick = component::onNewKeySaved,
        style = ButtonStyle.Post,
        enabled = model.canConfirmSaved,
    )
}
