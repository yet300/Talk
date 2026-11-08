package com.yet.talk.ui.screens.settings.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ActionRow
import com.yet.talk.ui.components.Avatar
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.ConfirmDialog
import com.yet.talk.ui.components.ListRow
import com.yet.talk.ui.components.SectionTitle
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.platform.copyText
import com.yet.talk.ui.screens.settings.dialog.KeyBox
import com.yet.talk.ui.screens.settings.dialog.SignOutDialog
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * Identity (`SettingsPanel`): who is signed in (`npub… · Private key on this device`) with Sign out, which asks first;
 * Edit profile; then Show private key, behind a confirmation with the warning, which reveals the `nsec` with Copy and
 * Hide. Leaving Settings hides the key again.
 */
@Composable
fun IdentitySection(model: SettingsComponent.Model, component: SettingsComponent) {
    var signingOut by rememberSaveable { mutableStateOf(false) }
    var confirmShow by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val copied = stringResource(MR.strings.action_copied)
    DisposableEffect(component) { onDispose { component.onHideKey() } }
    SectionTitle(stringResource(MR.strings.settings_identity))
    ListRow(
        title = model.me?.displayLabel ?: model.account?.npub?.shortNpub().orEmpty(),
        caption = listOfNotNull(model.account?.npub?.shortNpub(), stringResource(MR.strings.settings_signer_local)).joinToString(" · "),
        leading = { Avatar(model.me) },
        trailing = { TalkButton(stringResource(MR.strings.settings_sign_out), { signingOut = true }, ButtonStyle.Secondary, enabled = !model.isWorking) },
    )
    ActionRow(stringResource(MR.strings.action_edit_profile), component::onEditProfile, icon = TalkIcons.User)
    AnimatedContent(model.exportedKey != null && !signingOut, label = "key") { shown ->
        val key = model.exportedKey
        if (shown && key != null) {
            Column(Modifier.padding(MaterialTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                Text(stringResource(MR.strings.settings_key_warning), style = MaterialTheme.talkTypography.body, color = MaterialTheme.talkColors.error)
                KeyBox(key)
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                    TalkButton(stringResource(MR.strings.action_copy), { copyText(context, key, copied, sensitive = true) }, ButtonStyle.Secondary)
                    TalkButton(stringResource(MR.strings.settings_hide_key), component::onHideKey, ButtonStyle.Secondary)
                }
            }
        } else {
            ListRow(
                title = stringResource(MR.strings.settings_private_key),
                leading = null,
                trailing = { TalkButton(stringResource(MR.strings.settings_show_key), { confirmShow = true }, ButtonStyle.Secondary) },
            )
        }
    }
    if (confirmShow) {
        ConfirmDialog(
            title = stringResource(MR.strings.settings_show_key),
            body = stringResource(MR.strings.settings_key_warning),
            confirm = stringResource(MR.strings.settings_show_key),
            destructive = false,
            onConfirm = { confirmShow = false; component.onExportKey() },
            onDismiss = { confirmShow = false },
        )
    }
    if (signingOut) {
        SignOutDialog(
            exportedKey = model.exportedKey,
            onShowKey = component::onExportKey,
            onConfirm = { signingOut = false; component.onSignOut() },
            onDismiss = { signingOut = false; component.onHideKey() },
        )
    }
}
