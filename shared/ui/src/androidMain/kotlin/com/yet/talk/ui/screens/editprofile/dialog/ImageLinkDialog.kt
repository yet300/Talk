package com.yet.talk.ui.screens.editprofile.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import com.yet.talk.domain.rule.ProfileImage
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ConfirmDialog
import com.yet.talk.ui.components.TalkTextField
import com.yet.talk.ui.components.TextAction
import com.yet.talk.ui.theme.stringResource

/**
 * Asks for a GIPHY or Unsplash link for the avatar or banner (decision 13): Open GIPHY and Open Unsplash go to the
 * browser, the pasted link is checked when Use is pressed.
 */
@Composable
fun ImageLinkDialog(kind: ProfileImage, onSubmit: (String) -> Unit, onDismiss: () -> Unit) {
    var link by rememberSaveable { mutableStateOf("") }
    val uri = LocalUriHandler.current
    ConfirmDialog(
        title = stringResource(if (kind == ProfileImage.Avatar) MR.strings.edit_profile_avatar_link else MR.strings.edit_profile_banner_link),
        body = stringResource(MR.strings.edit_profile_link_body),
        confirm = stringResource(MR.strings.action_use),
        destructive = false,
        confirmEnabled = link.isNotBlank(),
        onConfirm = { onSubmit(link) },
        onDismiss = onDismiss,
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            TextAction(stringResource(MR.strings.edit_profile_open_giphy), { uri.openUri("https://giphy.com") })
            TextAction(stringResource(MR.strings.edit_profile_open_unsplash), { uri.openUri("https://unsplash.com") })
        }
        TalkTextField(
            link, { link = it },
            label = stringResource(MR.strings.edit_profile_link_label),
            placeholder = stringResource(MR.strings.edit_profile_link_placeholder),
            keyboardType = KeyboardType.Uri,
            onDone = { if (link.isNotBlank()) onSubmit(link) },
        )
    }
}
