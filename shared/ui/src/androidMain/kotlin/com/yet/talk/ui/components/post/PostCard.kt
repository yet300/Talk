package com.yet.talk.ui.components.post

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yet.talk.domain.model.moderation.HiddenReason
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.resources.*
import com.yet.talk.ui.components.Avatar
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.LocalModeration
import com.yet.talk.ui.components.LocalPostCards
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * The design's post card (`PostCard`): the repost line in Following, avatar, name, `handle · time`, "Replying to";
 * an article card (title, excerpt, `N min read #tags`) for a post starting with `# heading`, else a text card
 * (6 lines, Show more); images, the link, the quoted post; then the actions, or Sending… / Not sent with Retry.
 * Reported or blocked posts sit behind a row with Show.
 */
@Composable
fun PostCard(post: Post, onOpen: (Post) -> Unit, onOpenAuthor: (Post) -> Unit, actions: PostActionHandlers) {
    var revealed by rememberSaveable(post.id.value) { mutableStateOf(false) }
    val hidden = LocalModeration.current.hiddenReason(post.id, post.author.id)
    if (hidden != null && !revealed) return HiddenRow(hidden) { revealed = true }
    val colors = MaterialTheme.talkColors
    val spacing = MaterialTheme.spacing
    val summary = post.summary
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpen(post) }) {
        Column(Modifier.padding(horizontal = spacing.lg, vertical = spacing.md)) {
            post.repostedBy?.let { reposter ->
                Row(Modifier.padding(start = 24.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TalkIcon(TalkIcons.Repost, contentDescription = null, size = 16.dp, tint = colors.textSecondary)
                    Text(
                        stringResource(MR.strings.post_reposted, reposter.displayLabel),
                        style = MaterialTheme.talkTypography.small,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                Avatar(post.author, onClick = { onOpenAuthor(post) })
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PostHead(post, onEdited = { actions.onOpenRevisions(post) })
                    post.replyTo?.let {
                        Text(stringResource(MR.strings.post_replying_to, it.handle), style = MaterialTheme.talkTypography.body, color = colors.textSecondary)
                    }
                    if (summary.isArticle) {
                        Text(summary.title, style = MaterialTheme.talkTypography.headingMedium, color = colors.textPrimary)
                        if (summary.excerpt.isNotBlank()) {
                            Text(summary.excerpt, style = MaterialTheme.talkTypography.body, color = colors.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            (listOf(stringResource(MR.strings.post_read_time, summary.readMinutes)) + summary.tags.map { "#$it" }).joinToString("  "),
                            style = MaterialTheme.talkTypography.small,
                            color = colors.textSecondary,
                        )
                    } else if (summary.text.isNotBlank()) {
                        TextBody(summary.text)
                    }
                    if (post.images.isNotEmpty()) MediaGrid(post.images)
                    summary.link?.let { LinkCard(it) }
                    post.quoteOf?.let { QuotedCard(it, onOpen) }
                    when (post.publishState) {
                        PublishState.Pending -> Text(stringResource(MR.strings.post_pending), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
                        PublishState.Failed -> FailedRow(post)
                        PublishState.Published -> PostActions(post, actions)
                    }
                }
            }
        }
        HorizontalDivider(color = colors.border, thickness = MaterialTheme.sizes.hairline)
    }
}

/** A text card's body: up to six lines, then Show more expands it in place. */
@Composable
private fun TextBody(text: String) {
    var expanded by remember(text) { mutableStateOf(false) }
    var overflows by remember(text) { mutableStateOf(false) }
    val colors = MaterialTheme.talkColors
    Text(
        text,
        style = MaterialTheme.talkTypography.body,
        color = colors.textPrimary,
        maxLines = if (expanded) Int.MAX_VALUE else TEXT_LINES,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
    )
    if (overflows && !expanded) {
        Text(
            stringResource(MR.strings.post_show_more),
            style = MaterialTheme.talkTypography.body,
            color = colors.accent,
            modifier = Modifier.clickable(role = Role.Button) { expanded = true },
        )
    }
}

/** Not sent, with Retry: publishes the stored event again (to the relays still owed it). */
@Composable
private fun FailedRow(post: Post) {
    val cards = LocalPostCards.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
        Text(stringResource(MR.strings.post_failed), style = MaterialTheme.talkTypography.bodyBold, color = MaterialTheme.talkColors.error)
        TalkButton(stringResource(MR.strings.action_retry), { cards.retry(post) }, ButtonStyle.Secondary)
    }
}

/** A post the viewer's network reported, or whose author they blocked: the row, with Show to reveal it. */
@Composable
private fun HiddenRow(reason: HiddenReason, onShow: () -> Unit) {
    val colors = MaterialTheme.talkColors
    Column {
        Row(
            Modifier.fillMaxWidth().background(colors.surface).padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            TalkIcon(TalkIcons.EyeOff, contentDescription = null, size = 18.dp, tint = colors.textSecondary)
            Text(
                stringResource(if (reason == HiddenReason.Blocked) MR.strings.moderation_hidden_blocked else MR.strings.moderation_hidden),
                style = MaterialTheme.talkTypography.body,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(MR.strings.action_show), style = MaterialTheme.talkTypography.bodyBold, color = colors.accent, modifier = Modifier.clickable(role = Role.Button, onClick = onShow))
        }
        HorizontalDivider(color = colors.border, thickness = MaterialTheme.sizes.hairline)
    }
}

private const val TEXT_LINES = 6

