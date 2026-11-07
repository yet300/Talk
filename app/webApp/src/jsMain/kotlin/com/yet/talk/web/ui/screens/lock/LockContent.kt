package com.yet.talk.web.ui.screens.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.feature.auth.lock.LockComponent
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.ConfirmDialog
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.TextField
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * The lock screen (`AppLock`, `Lock-Web`): "Talk is locked", the passcode and Unlock; Forgot passcode? erases the
 * account here (it can't be recovered) after a warning.
 */
@Composable
fun LockContent(component: LockComponent) {
    val model by component.model.subscribeAsState()
    var forgot by remember { mutableStateOf(false) }
    val title = stringResource(MR.strings.lock_title)
    Div({ classes("talk", "t-auth-page") }) {
        Section({ classes("t-part", "t-auth", "t-lock"); attr("aria-label", title) }) {
            Div({ classes("t-wordmark") }) { Text(stringResource(MR.strings.app_name)) }
            Span({ classes("t-muted") }) { TalkIcon(TalkIcons.Lock, size = 48f) }
            H1({ classes("t-h1", "t-flush") }) { Text(title) }
            model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
            TextField(
                value = model.passcode,
                onValueChange = component::onPasscodeChanged,
                label = stringResource(MR.strings.lock_passcode),
                placeholder = "",
                id = "lock-passcode",
                secret = true,
                onEnter = component::onUnlock,
            )
            TalkButton(
                stringResource(if (model.isUnlocking) MR.strings.lock_unlocking else MR.strings.lock_unlock),
                component::onUnlock,
                ButtonStyle.Post,
                attrs = { if (model.isUnlocking || model.passcode.isEmpty()) attr("disabled", "") },
            )
            Button({
                classes("t-text-btn")
                type(ButtonType.Button)
                onClick { forgot = true }
            }) { Text(stringResource(MR.strings.lock_forgot)) }
        }
    }
    if (forgot) {
        ConfirmDialog(
            title = stringResource(MR.strings.lock_forgot_title),
            body = stringResource(MR.strings.lock_forgot_body),
            confirm = stringResource(MR.strings.lock_forgot_confirm),
            onConfirm = { forgot = false; component.onForgot() },
            onDismiss = { forgot = false },
        )
    }
}
