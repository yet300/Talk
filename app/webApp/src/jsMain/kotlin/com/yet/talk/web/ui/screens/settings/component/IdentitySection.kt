package com.yet.talk.web.ui.screens.settings.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.copyToClipboard
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.LocalToaster
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.list.SectionTitle
import com.yet.talk.web.ui.screens.settings.dialog.SignOutDialog
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Identity (`SettingsPanel`): who is signed in (`npub… · Private key on this device`) with Sign out, which asks
 * first; then the Private key row: Show private key reveals the `nsec` with a warning, Copy and Hide.
 */
@Composable
fun IdentitySection(model: SettingsComponent.Model, component: SettingsComponent) {
    var signingOut by remember { mutableStateOf(false) }
    SectionTitle(stringResource(MR.strings.settings_identity))
    Div({ classes("t-row") }) {
        Avatar(model.me)
        Div({ classes("t-row-text") }) {
            Div({ classes("x-name") }) { Text(model.me?.displayLabel ?: model.account?.npub?.shortNpub().orEmpty()) }
            Div({ classes("t-caption") }) {
                Text(listOfNotNull(model.account?.npub?.shortNpub(), stringResource(MR.strings.settings_signer_local)).joinToString(" · "))
            }
        }
        TalkButton(stringResource(MR.strings.settings_sign_out), { signingOut = true }, ButtonStyle.Secondary, enabled = !model.isWorking)
    }
    A(href = "/settings/profile", attrs = {
        classes("t-row")
        onClick { it.preventDefault(); component.onEditProfile() }
    }) { Span({ classes("t-row-text", "x-name") }) { Text(stringResource(MR.strings.action_edit_profile)) } }
    if (signingOut) {
        // The dialog shows the key only while it is open.
        SignOutDialog(
            exportedKey = model.exportedKey,
            onShowKey = component::onExportKey,
            onConfirm = { signingOut = false; component.onSignOut() },
            onDismiss = { signingOut = false; component.onHideKey() },
        )
    } else {
        PrivateKeyRow(model.exportedKey, component)
    }
}

@Composable
private fun PrivateKeyRow(key: String?, component: SettingsComponent) {
    val toaster = LocalToaster.current
    val copied = stringResource(MR.strings.action_copied)
    if (key == null) {
        Div({ classes("t-row") }) {
            Span({ classes("t-row-text", "x-name") }) { Text(stringResource(MR.strings.settings_private_key)) }
            TalkButton(stringResource(MR.strings.settings_show_key), component::onExportKey, ButtonStyle.Secondary)
        }
    } else {
        Div({ classes("t-key") }) {
            P({ classes("x-alert", "t-flush"); attr("role", "alert") }) { Text(stringResource(MR.strings.settings_key_warning)) }
            Div({ classes("t-mono-box") }) { Text(key) }
            Div({ classes("t-row-actions") }) {
                TalkButton(stringResource(MR.strings.action_copy), { copyToClipboard(key); toaster.show(copied) }, ButtonStyle.Secondary)
                TalkButton(stringResource(MR.strings.settings_hide_key), component::onHideKey, ButtonStyle.Secondary)
            }
        }
    }
}
