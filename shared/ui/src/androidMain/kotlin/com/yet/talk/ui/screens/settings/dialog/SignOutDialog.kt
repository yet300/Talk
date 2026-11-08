package com.yet.talk.ui.screens.settings.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.ConfirmDialog
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.platform.SecureScreen
import com.yet.talk.ui.platform.copyText
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * "Sign out?" (`ConfirmDialog` with the key, design pass 2): without the key the account can't be signed back in to,
 * so the dialog offers Show private key and Copy (flagged sensitive) before the red Sign out. The key is kept out of
 * screenshots while it shows.
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
    val context = LocalContext.current
    val copied = stringResource(MR.strings.action_copied)
    ConfirmDialog(title = title, body = body, confirm = confirm, onConfirm = onConfirm, onDismiss = onDismiss) {
        if (exportedKey == null) {
            TalkButton(stringResource(MR.strings.settings_show_key), onShowKey, ButtonStyle.Secondary)
        } else {
            KeyBox(exportedKey)
            TalkButton(stringResource(MR.strings.action_copy), { copyText(context, exportedKey, copied, sensitive = true) }, ButtonStyle.Secondary)
        }
    }
}

/** The `nsec` in a monospace box (`t-mono-box`); the window is secure while it is composed. */
@Composable
fun KeyBox(key: String) {
    SecureScreen()
    Text(
        key,
        style = MaterialTheme.talkTypography.code,
        color = MaterialTheme.talkColors.textPrimary,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.talkColors.surface, MaterialTheme.shapes.small).padding(MaterialTheme.spacing.md),
    )
}
