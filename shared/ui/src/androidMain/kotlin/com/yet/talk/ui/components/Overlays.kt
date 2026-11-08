package com.yet.talk.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yet.talk.domain.model.moderation.ReportReason
import com.yet.talk.domain.model.moderation.ReportTarget
import com.yet.talk.resources.*
import com.yet.talk.ui.platform.toast
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import dev.icerock.moko.resources.ImageResource
import kotlinx.coroutines.launch

/**
 * The design's bottom sheet (`BottomSheet`, decision 3: More actions on the phone): slides up over a scrim, drags and
 * flings down, and closes on back (with the predictive back gesture on Android 14+, which Material's sheet handles).
 * [content] gets a `close` that animates the sheet away before [onDismiss] runs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalkBottomSheet(onDismiss: () -> Unit, title: String? = null, content: @Composable ColumnScope.(close: (then: () -> Unit) -> Unit) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: (() -> Unit) -> Unit = { then ->
        scope.launch { state.hide() }.invokeOnCompletion {
            onDismiss()
            then()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = MaterialTheme.talkColors.bg,
        contentColor = MaterialTheme.talkColors.textPrimary,
        scrimColor = MaterialTheme.talkColors.modalScrim,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = MaterialTheme.spacing.lg)) {
            if (title != null) {
                Text(title, style = MaterialTheme.talkTypography.headingMedium, modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl, vertical = MaterialTheme.spacing.sm))
            }
            content(close)
        }
    }
}

/** One action in a sheet (`t-menu-row`): icon, label, an optional note under it; red when it destroys something. */
@Composable
fun SheetRow(label: String, onClick: () -> Unit, icon: ImageResource? = null, note: String? = null, danger: Boolean = false) {
    val colors = MaterialTheme.talkColors
    val tint = if (danger) colors.error else colors.textPrimary
    Row(
        Modifier.fillMaxWidth().heightIn(min = MaterialTheme.sizes.touchMin + 8.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = MaterialTheme.spacing.xl, vertical = MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
    ) {
        if (icon != null) TalkIcon(icon, contentDescription = null, tint = tint)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.talkTypography.bodyBold, color = tint)
            if (note != null) Text(note, style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
        }
    }
}

/**
 * The design's confirm dialog (`ConfirmDialog`): a title, body, Cancel and a confirm that is red when it destroys
 * something. Back and a tap outside cancel. [extra] sits under the body (Sign out's key step).
 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
    confirmEnabled: Boolean = true,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.talkColors.bg,
        title = { Text(title, style = MaterialTheme.talkTypography.headingLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                Text(body, style = MaterialTheme.talkTypography.body, color = MaterialTheme.talkColors.textSecondary)
                extra?.invoke(this)
            }
        },
        confirmButton = { TextAction(confirm, onConfirm, danger = destructive, enabled = confirmEnabled) },
        dismissButton = { TextAction(stringResource(MR.strings.action_cancel), onDismiss) },
    )
}

/**
 * Report (NIP-56): the public-report note and the reasons (decision 18), in a sheet; then the "Reported. Your report
 * is public." toast.
 */
@Composable
fun ReportSheet(target: ReportTarget, onDismiss: () -> Unit) {
    val moderation = LocalModerationActions.current
    val context = LocalContext.current
    val reported = stringResource(MR.strings.toast_reported)
    TalkBottomSheet(onDismiss = onDismiss, title = stringResource(MR.strings.report_title)) { close ->
        Text(
            stringResource(MR.strings.report_note),
            style = MaterialTheme.talkTypography.body,
            color = MaterialTheme.talkColors.textSecondary,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl, vertical = MaterialTheme.spacing.sm),
        )
        ReportReason.entries.forEach { reason ->
            SheetRow(stringResource(reason.label), onClick = {
                close {
                    moderation?.report(target, reason)
                    toast(context, reported)
                }
            })
        }
    }
}

val ReportReason.label
    get() = when (this) {
        ReportReason.Nudity -> MR.strings.report_nudity
        ReportReason.Spam -> MR.strings.report_spam
        ReportReason.Illegal -> MR.strings.report_illegal
        ReportReason.Impersonation -> MR.strings.report_impersonation
        ReportReason.Other -> MR.strings.report_other
    }
