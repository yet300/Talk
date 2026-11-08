package com.yet.talk.ui.screens.video.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.video.VideoComment
import com.yet.talk.feature.video.VideoComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.Avatar
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.TalkMarkdown
import com.yet.talk.ui.components.TalkTextField
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import com.yet.talk.ui.theme.text

/** "Add a comment" and Reply (NIP-22); comments are top-level only and have no actions (decision 16). */
@Composable
fun CommentField(model: VideoComponent.Model, component: VideoComponent) {
    Row(Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        TalkTextField(
            value = model.comment,
            onValueChange = component::onCommentChanged,
            placeholder = stringResource(MR.strings.video_comment_placeholder),
            modifier = Modifier.weight(1f),
            pill = true,
            imeAction = ImeAction.Send,
            onDone = { if (model.canSend) component.onSendComment() },
        )
        TalkButton(stringResource(MR.strings.action_reply), component::onSendComment, ButtonStyle.Post, enabled = model.canSend)
    }
}

@Composable
fun CommentCard(comment: VideoComment, onOpenAuthor: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.talkColors
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
            Avatar(comment.author, onClick = onOpenAuthor)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(comment.author.displayLabel, style = MaterialTheme.talkTypography.bodyBold, color = colors.textPrimary, modifier = Modifier.clickable(onClick = onOpenAuthor))
                    Text("${comment.author.handle} · ${comment.createdAt.toRelativeDesc().text()}", style = MaterialTheme.talkTypography.body, color = colors.textSecondary, maxLines = 1)
                }
                when (comment.publishState) {
                    PublishState.Pending -> Text(stringResource(MR.strings.post_pending), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
                    PublishState.Failed -> Text(stringResource(MR.strings.post_failed), style = MaterialTheme.talkTypography.small, color = colors.error)
                    PublishState.Published -> Unit
                }
                TalkMarkdown(comment.content)
            }
        }
        HorizontalDivider(color = colors.border, thickness = MaterialTheme.sizes.hairline)
    }
}
