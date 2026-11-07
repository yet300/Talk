package com.yet.talk.web.ui.screens.settings.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.ConfirmDialog
import com.yet.talk.web.ui.components.LocalToaster
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.Tabs
import com.yet.talk.web.ui.components.TextField
import com.yet.talk.web.ui.components.list.SectionTitle
import com.yet.talk.web.ui.screens.settings.dialog.SignOutDialog
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Security (`SecurityPanel`): App lock (Off · Passcode, on the web), Erase chats from this device, Erase everything.
 * Hide chats in screenshots is Android only and comes with the Android app.
 */
@Composable
fun SecuritySection(model: SettingsComponent.Model, component: SettingsComponent) {
    var dialog by remember { mutableStateOf<SecurityDialog?>(null) }
    val toaster = LocalToaster.current
    val erased = stringResource(MR.strings.toast_chats_erased)
    SectionTitle(stringResource(MR.strings.settings_security))
    if (model.lock.passcodeSupported) {
        Div({ classes("t-row", "t-row-flat") }) {
            Span({ classes("t-row-text") }) {
                Span({ classes("x-name", "t-block") }) { Text(stringResource(MR.strings.security_app_lock)) }
                Span({ classes("t-caption", "t-block") }) { Text(stringResource(MR.strings.security_lock_caption)) }
            }
        }
        Tabs(
            items = listOf(false, true),
            selected = model.lock.enabled,
            label = { stringResource(if (it) MR.strings.security_passcode else MR.strings.security_off) },
            onSelect = { on ->
                when {
                    on && !model.lock.enabled -> dialog = SecurityDialog.SetPasscode
                    !on && model.lock.enabled -> component.onRemovePasscode()
                }
            },
            ariaLabel = stringResource(MR.strings.security_app_lock),
        )
        if (model.isWorking && model.lock.enabled.not() && dialog == null) {
            P({ classes("t-caption", "t-info-note"); attr("role", "status") }) { Text(stringResource(MR.strings.security_encrypting)) }
        }
    }
    DangerRow(stringResource(MR.strings.security_erase_chats)) { dialog = SecurityDialog.EraseChats }
    DangerRow(stringResource(MR.strings.security_erase_all)) { dialog = SecurityDialog.EraseEverything }

    when (dialog) {
        SecurityDialog.SetPasscode -> SetPasscodeDialog(
            onConfirm = { passcode, again ->
                dialog = null
                component.onSetPasscode(passcode, again)
            },
            onDismiss = { dialog = null },
        )
        SecurityDialog.EraseChats -> ConfirmDialog(
            title = stringResource(MR.strings.security_erase_chats_title),
            body = stringResource(MR.strings.security_erase_chats_body),
            confirm = stringResource(MR.strings.security_erase_chats_confirm),
            onConfirm = {
                dialog = null
                component.onEraseChats()
                toaster.show(erased)
            },
            onDismiss = { dialog = null },
        )
        // As Sign out: the key goes too, so the dialog offers to show it first.
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

private enum class SecurityDialog { SetPasscode, EraseChats, EraseEverything }

@Composable
private fun DangerRow(label: String, onClick: () -> Unit) {
    Button({
        classes("t-menu-row", "t-danger", "t-security-row")
        type(ButtonType.Button)
        onClick { onClick() }
    }) { Text(label) }
}

/** "Set a passcode": twice, with what forgetting it means. */
@Composable
private fun SetPasscodeDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var passcode by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val title = stringResource(MR.strings.security_set_title)
    Div({ classes("x-scrim", "t-layer"); onClick { onDismiss() } }) {
        Div({
            classes("x-modal", "t-dialog")
            attr("role", "dialog")
            attr("aria-modal", "true")
            attr("aria-label", title)
            onClick { it.stopPropagation() }
        }) {
            H2({ classes("t-h1", "t-flush") }) { Text(title) }
            P({ classes("t-muted", "t-flush") }) { Text(stringResource(MR.strings.security_set_body)) }
            TextField(passcode, { passcode = it }, stringResource(MR.strings.security_passcode), "", "new-passcode", secret = true)
            TextField(again, { again = it }, stringResource(MR.strings.security_passcode_again), "", "new-passcode-again", secret = true) {
                if (passcode.isNotEmpty()) onConfirm(passcode, again)
            }
            TalkButton(stringResource(MR.strings.security_turn_on), { onConfirm(passcode, again) }, ButtonStyle.Primary, attrs = {
                classes("t-btn-44")
                if (passcode.isEmpty()) attr("disabled", "")
            })
            TalkButton(stringResource(MR.strings.action_cancel), onDismiss, ButtonStyle.Secondary, attrs = { classes("t-btn-44") })
        }
    }
}
