package com.yet.talk.ui.screens.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.auth.lock.LockComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.ConfirmDialog
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.NetworkBar
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.TalkTextField
import com.yet.talk.ui.components.TextAction
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * The passcode lock screen (`Lock-Mobile`): "Talk is locked", the passcode and Unlock; Forgot passcode? erases the
 * account here after a warning. Android locks with the device instead ([DeviceLockGate]), so the root shows this
 * only when the stored key is a passcode-encrypted one.
 */
@Composable
fun LockContent(component: LockComponent) {
    val model by component.model.subscribeAsState()
    var forgot by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        NetworkBar()
        Column(
            Modifier.align(Alignment.Center).widthIn(max = 420.dp).padding(MaterialTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
        ) {
            Text(stringResource(MR.strings.app_name), style = MaterialTheme.talkTypography.wordmark, color = MaterialTheme.talkColors.textPrimary)
            TalkIcon(TalkIcons.Lock, null, size = MaterialTheme.sizes.iconEmpty, tint = MaterialTheme.talkColors.textSecondary)
            Text(stringResource(MR.strings.lock_title), style = MaterialTheme.talkTypography.headingLarge, color = MaterialTheme.talkColors.textPrimary)
            ErrorRow(model.error, onDismiss = component::onErrorShown)
            TalkTextField(model.passcode, component::onPasscodeChanged, label = stringResource(MR.strings.lock_passcode), secret = true, onDone = component::onUnlock)
            TalkButton(
                stringResource(if (model.isUnlocking) MR.strings.lock_unlocking else MR.strings.lock_unlock),
                component::onUnlock, ButtonStyle.Post, enabled = !model.isUnlocking && model.passcode.isNotEmpty(), fullWidth = true,
            )
            TextAction(stringResource(MR.strings.lock_forgot), { forgot = true })
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
