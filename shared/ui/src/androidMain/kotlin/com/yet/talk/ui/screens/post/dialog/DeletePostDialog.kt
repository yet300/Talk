package com.yet.talk.ui.screens.post.dialog

import androidx.compose.runtime.Composable
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ConfirmDialog
import com.yet.talk.ui.theme.stringResource

/** "Delete post?" (decision 3: Delete asks first); a deletion cannot be undone on relays. */
@Composable
fun DeletePostDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = stringResource(MR.strings.post_delete_title),
        body = stringResource(MR.strings.post_delete_body),
        confirm = stringResource(MR.strings.action_delete),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
