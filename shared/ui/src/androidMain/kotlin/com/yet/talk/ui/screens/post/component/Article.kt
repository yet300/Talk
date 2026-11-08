package com.yet.talk.ui.screens.post.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yet.talk.domain.model.post.Post
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.Avatar
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.FollowButton
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.TalkMarkdown
import com.yet.talk.ui.components.post.PostActionHandlers
import com.yet.talk.ui.components.post.PostActions
import com.yet.talk.ui.components.post.QuotedCard
import com.yet.talk.ui.platform.copyText
import com.yet.talk.ui.platform.shareText
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * The article (`Article`): Show the parent for a reply; the title from the markdown, the author row with Follow, the
 * action row (Reply goes to the reply field, or the replies pane in two panes), Share and Revisions, the quoted post,
 * the body, tags, and the post's address (tap to copy).
 */
@Composable
fun Article(post: Post, model: PostComponent.Model, component: PostComponent, onReply: () -> Unit) {
    val colors = MaterialTheme.talkColors
    val spacing = MaterialTheme.spacing
    val (title, body) = remember(post.content) { splitTitle(post.content) }
    val context = LocalContext.current
    val copied = stringResource(MR.strings.toast_link_copied)
    val actions = remember(component, onReply) {
        PostActionHandlers({ onReply() }, component::onRepost, component::onLike, component::onBookmark, { component.onOpenRevisions() })
    }
    Column(Modifier.padding(horizontal = spacing.lg, vertical = spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        post.thread?.let { thread ->
            Text(
                stringResource(MR.strings.post_show_parent),
                style = MaterialTheme.talkTypography.body,
                color = colors.accent,
                modifier = Modifier.clickable(role = Role.Button) { component.onOpenPost(thread.parentId) },
            )
        }
        if (title != null) Text(title, style = MaterialTheme.talkTypography.display, color = colors.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            Avatar(post.author, onClick = { component.onOpenAuthor(post.author.id) })
            Column(Modifier.weight(1f)) {
                Text(post.author.displayLabel, style = MaterialTheme.talkTypography.bodyBold, color = colors.textPrimary)
                Text(
                    listOf(post.author.handle, post.createdAt.toDateText(), stringResource(MR.strings.post_read_time, post.summary.readMinutes)).joinToString(" · "),
                    style = MaterialTheme.talkTypography.small,
                    color = colors.textSecondary,
                )
                if (post.editedAt != null) {
                    Text(
                        stringResource(MR.strings.post_edited),
                        style = MaterialTheme.talkTypography.small,
                        color = colors.accent,
                        modifier = Modifier.clickable(role = Role.Button, onClick = component::onOpenRevisions),
                    )
                }
            }
            if (!model.isMine) FollowButton(model.isFollowingAuthor, component::onFollowAuthor)
        }
        PostActions(post, actions)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            TalkButton(stringResource(MR.strings.action_share), { shareText(context, model.shareLink) }, ButtonStyle.Secondary)
            TalkButton(stringResource(MR.strings.action_revisions), component::onOpenRevisions, ButtonStyle.Secondary)
        }
        post.quoteOf?.let { QuotedCard(it, onOpen = { quoted -> component.onOpenPost(quoted.id) }) }
        TalkMarkdown(body)
        if (post.summary.tags.isNotEmpty()) {
            Text(post.summary.tags.joinToString("  ") { "#$it" }, style = MaterialTheme.talkTypography.body, color = colors.accent)
        }
        Row(
            Modifier.clickable(role = Role.Button) { copyText(context, model.shareLink, copied) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TalkIcon(TalkIcons.Copy, contentDescription = stringResource(MR.strings.post_copy_address), size = 18.dp, tint = colors.textSecondary)
            Text(model.shareLink.shortNpub(), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
        }
    }
    HorizontalDivider(color = colors.border, thickness = MaterialTheme.sizes.hairline)
}

/** A leading `# Title` line, and the markdown after it; no title when the post does not start with one. */
private fun splitTitle(content: String): Pair<String?, String> {
    val trimmed = content.trimStart()
    val first = trimmed.lineSequence().firstOrNull().orEmpty()
    return if (first.startsWith("# ")) first.removePrefix("# ").trim() to trimmed.removePrefix(first).trimStart('\n') else null to content
}
