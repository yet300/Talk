package com.yet.talk.web.ui.screens.post.dialog

import androidx.compose.runtime.Composable
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.ConfirmDialog
import com.yet.talk.web.ui.theme.stringResource

/** "Delete this post?": relays are asked to delete it; some may keep a copy. */
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
