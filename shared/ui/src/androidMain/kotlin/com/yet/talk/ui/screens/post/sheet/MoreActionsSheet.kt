package com.yet.talk.ui.screens.post.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yet.talk.domain.model.moderation.ReportTarget
import com.yet.talk.domain.model.post.Post
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ReportSheet
import com.yet.talk.ui.components.SheetRow
import com.yet.talk.ui.components.TalkBottomSheet
import com.yet.talk.ui.platform.copyText
import com.yet.talk.ui.platform.shareText
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.stringResource

/**
 * More actions (`MoreFlow`, decision 3: a bottom sheet on the phone): Quote, Copy link, Share; Edit and Delete on your
 * own post, Report on someone else's (which goes on to the reasons).
 */
@Composable
fun MoreActionsSheet(post: Post, model: PostComponent.Model, component: PostComponent, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val copied = stringResource(MR.strings.toast_link_copied)
    var reporting by rememberSaveable { mutableStateOf(false) }
    if (reporting) {
        ReportSheet(ReportTarget.Post(post.id, post.author.id), onDismiss = { reporting = false; onDismiss() })
        return
    }
    TalkBottomSheet(onDismiss = onDismiss) { close ->
        SheetRow(stringResource(MR.strings.action_quote), icon = TalkIcons.Repost, onClick = { close { component.onQuote(post) } })
        SheetRow(stringResource(MR.strings.post_copy_link), icon = TalkIcons.Link, onClick = { close { copyText(context, model.shareLink, copied) } })
        SheetRow(stringResource(MR.strings.action_share), icon = TalkIcons.Share, onClick = { close { shareText(context, model.shareLink) } })
        if (model.isMine) {
            SheetRow(stringResource(MR.strings.action_edit), icon = TalkIcons.Compose, onClick = { close(component::onEdit) })
            SheetRow(stringResource(MR.strings.action_delete), icon = TalkIcons.Trash, danger = true, onClick = { close(onDelete) })
        } else {
            SheetRow(stringResource(MR.strings.action_report), icon = TalkIcons.Report, danger = true, onClick = { reporting = true })
        }
    }
}
