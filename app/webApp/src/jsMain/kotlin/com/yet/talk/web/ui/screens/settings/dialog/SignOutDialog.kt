package com.yet.talk.web.ui.screens.settings.dialog

import androidx.compose.runtime.Composable
import com.yet.talk.resources.*
import com.yet.talk.web.browser.copyToClipboard
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.ConfirmDialog
import com.yet.talk.web.ui.components.LocalToaster
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

/**
 * "Sign out?" (`ConfirmDialog` with the key): without the key the account can't be signed back in to, so the
 * dialog offers Show private key and Copy before the red Sign out.
 */
@Composable
fun SignOutDialog(
    exportedKey: String?,
    onShowKey: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(MR.strings.settings_sign_out_title),
    body: String = stringResource(MR.strings.settings_sign_out_body),
    confirm: String = stringResource(MR.strings.settings_sign_out),
) {
    val toaster = LocalToaster.current
    val copied = stringResource(MR.strings.action_copied)
    val showKey = stringResource(MR.strings.settings_show_key)
    val copy = stringResource(MR.strings.action_copy)
    ConfirmDialog(
        title = title,
        body = body,
        confirm = confirm,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        extra = {
            if (exportedKey == null) {
                TalkButton(showKey, onShowKey, ButtonStyle.Secondary, attrs = { classes("t-btn-44") })
            } else {
                Div({ classes("t-mono-box") }) { Text(exportedKey) }
                TalkButton(copy, { copyToClipboard(exportedKey); toaster.show(copied) }, ButtonStyle.Secondary, attrs = { classes("t-btn-44") })
            }
        },
    )
}
