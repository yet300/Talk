package com.yet.talk.ui.screens.settings.component

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ActionRow
import com.yet.talk.ui.components.ConfirmDialog
import com.yet.talk.ui.components.SectionTitle
import com.yet.talk.ui.components.SwitchRow
import com.yet.talk.ui.lock.DeviceLock
import com.yet.talk.ui.lock.LockSession
import com.yet.talk.ui.platform.toast
import com.yet.talk.ui.screens.settings.dialog.SignOutDialog
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.stringResource

/**
 * Security (`SecurityPanel` on Android, decision 24): Device lock (biometrics or the screen lock; turning it on asks
 * for it once, so nobody locks themselves out), Hide in screenshots (`FLAG_SECURE` on chats and the key), Erase chats
 * from this device, and Erase everything (with the key step, as Sign out). Talk's own passcode is the web's.
 */
@Composable
fun SecuritySection(model: SettingsComponent.Model, component: SettingsComponent) {
    var dialog by rememberSaveable { mutableStateOf<SecurityDialog?>(null) }
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val available = remember(context) { DeviceLock.isAvailable(context) }
    val erased = stringResource(MR.strings.toast_chats_erased)
    val title = stringResource(MR.strings.security_device_lock)
    val subtitle = stringResource(MR.strings.lock_device_body)
    SectionTitle(stringResource(MR.strings.settings_security))
    SwitchRow(
        title = title,
        caption = stringResource(if (available || model.privacy.deviceLock) MR.strings.security_device_lock_caption else MR.strings.security_device_lock_unavailable),
        checked = model.privacy.deviceLock,
        enabled = available || model.privacy.deviceLock,
        icon = TalkIcons.Lock,
        onCheckedChange = { on ->
            if (!on) {
                component.onDeviceLockChanged(false)
            } else if (activity != null) {
                LockSession.awayOnPurpose = true
                DeviceLock.authenticate(activity, title, subtitle) { ok ->
                    LockSession.awayOnPurpose = false
                    if (ok) {
                        LockSession.unlocked = true
                        component.onDeviceLockChanged(true)
                    }
                }
            }
        },
    )
    SwitchRow(
        title = stringResource(MR.strings.security_hide_screenshots),
        caption = stringResource(MR.strings.security_hide_screenshots_caption),
        checked = model.privacy.hideInScreenshots,
        icon = TalkIcons.EyeOff,
        onCheckedChange = component::onHideInScreenshotsChanged,
    )
    ActionRow(stringResource(MR.strings.security_erase_chats), { dialog = SecurityDialog.EraseChats }, icon = TalkIcons.Trash, danger = true)
    ActionRow(stringResource(MR.strings.security_erase_all), { dialog = SecurityDialog.EraseEverything }, icon = TalkIcons.Trash, danger = true)
    when (dialog) {
        SecurityDialog.EraseChats -> ConfirmDialog(
            title = stringResource(MR.strings.security_erase_chats_title),
            body = stringResource(MR.strings.security_erase_chats_body),
            confirm = stringResource(MR.strings.security_erase_chats_confirm),
            onConfirm = { dialog = null; component.onEraseChats(); toast(context, erased) },
            onDismiss = { dialog = null },
        )
        SecurityDialog.EraseEverything -> SignOutDialog(
            exportedKey = model.exportedKey,
            onShowKey = component::onExportKey,
            onConfirm = { dialog = null; component.onEraseEverything() },
            onDismiss = { dialog = null; component.onHideKey() },
            title = stringResource(MR.strings.security_erase_all_title),
            body = stringResource(MR.strings.security_erase_all_body),
            confirm = stringResource(MR.strings.security_erase_all),
        )
        null -> Unit
    }
}

/** The confirmation open over Security (saved across rotation). */
private enum class SecurityDialog { EraseChats, EraseEverything }
