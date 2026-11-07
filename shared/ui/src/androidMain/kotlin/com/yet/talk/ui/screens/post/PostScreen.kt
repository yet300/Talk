package com.yet.talk.ui.screens.post

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.domain.model.moderation.ReportReason
import com.yet.talk.domain.model.moderation.ReportTarget
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.ThreadReply
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.Avatar
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.LocalModeration
import com.yet.talk.ui.components.LocalModerationActions
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.TalkMarkdown
import com.yet.talk.ui.components.glass.GlassIconButton
import com.yet.talk.ui.components.glass.GlassTopBar
import com.yet.talk.ui.components.glass.LocalGlassSource
import com.yet.talk.ui.components.list.EmptyState
import com.yet.talk.ui.components.post.PostActionHandlers
import com.yet.talk.ui.components.post.PostActions
import com.yet.talk.ui.components.post.PostHead
import com.yet.talk.ui.components.post.QuotedCard
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.message
import com.yet.talk.ui.theme.pluralResource
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkShapes
import com.yet.talk.ui.theme.talkTypography
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * The reader (`Reader · Mobile`): the article edge to edge under a glass bar with Back and More; its title, author
 * with Follow, the actions, the quoted post, the markdown, tags and the post's address; then the replies with a
 * reply field.
 */
@Composable
fun PostScreen(component: PostComponent) {
    val model by component.model.subscribeAsState()
    val glass = rememberHazeState()
    val replyFocus = remember { FocusRequester() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val sizes = MaterialTheme.sizes
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + sizes.barHeight
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + MaterialTheme.spacing.xl
    CompositionLocalProvider(LocalGlassSource provides glass) {
        Box(Modifier.fillMaxSize().imePadding()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.talkColors.bg).hazeSource(glass),
                contentPadding = PaddingValues(top = top, bottom = bottom),
            ) {
                model.error?.let { error ->
                    item { Text(error.message(), style = MaterialTheme.talkTypography.body, color = MaterialTheme.talkColors.error, modifier = Modifier.padding(MaterialTheme.spacing.lg)) }
                }
                val post = model.post
                when {
                    post != null -> {
                        item(key = "article") { Article(post, model, component, onReply = { replyFocus.requestFocus() }) }
                        item(key = "replies") { RepliesHeader(model, component, replyFocus) }
                        items(model.replies, key = { it.post.id.value }) { ReplyCard(it, component) }
                    }
                    model.isLoading -> item {
                        Box(Modifier.fillMaxWidth().padding(MaterialTheme.spacing.xxl), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.talkColors.accent)
                        }
                    }
                    else -> item { EmptyState(title = stringResource(MR.strings.post_unavailable)) }
                }
            }
            GlassTopBar(
                leading = { GlassIconButton(TalkIcons.Back, stringResource(MR.strings.action_back), component::onBack) },
                title = {
                    Text(stringResource(MR.strings.post_title), style = MaterialTheme.talkTypography.headingMedium, color = MaterialTheme.talkColors.textPrimary, modifier = Modifier.weight(1f))
                },
                trailing = { model.post?.let { MoreActions(it, model, component, onDelete = { confirmDelete = true }) } },
            )
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(MR.strings.post_delete_title)) },
            text = { Text(stringResource(MR.strings.post_delete_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; component.onDelete() }) {
                    Text(stringResource(MR.strings.action_delete), color = MaterialTheme.talkColors.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(MR.strings.action_cancel)) } },
        )
    }
}

@Composable
private fun Article(post: Post, model: PostComponent.Model, component: PostComponent, onReply: () -> Unit) {
    val colors = MaterialTheme.talkColors
    val spacing = MaterialTheme.spacing
    val (title, body) = remember(post.content) { splitTitle(post.content) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val linkCopied = stringResource(MR.strings.toast_link_copied)
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
        // The author, with Follow on someone else's post.
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
            if (!model.isMine) {
                TalkButton(
                    stringResource(if (model.isFollowingAuthor) MR.strings.action_following else MR.strings.action_follow),
                    component::onFollowAuthor,
                    if (model.isFollowingAuthor) ButtonStyle.Secondary else ButtonStyle.Primary,
                )
            }
        }
        // Reply goes to the field below; Share hands out the post's address.
        PostActions(post, PostActionHandlers({ onReply() }, component::onRepost, component::onLike, component::onBookmark, { component.onOpenRevisions() }))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            TalkButton(stringResource(MR.strings.action_share), { share(context, model.shareLink) }, ButtonStyle.Secondary)
            TalkButton(stringResource(MR.strings.action_revisions), component::onOpenRevisions, ButtonStyle.Secondary)
        }
        post.quoteOf?.let { QuotedCard(it, onOpen = { quoted -> component.onOpenPost(quoted.id) }) }
        TalkMarkdown(body)
        if (post.summary.tags.isNotEmpty()) {
            Text(post.summary.tags.joinToString("  ") { "#$it" }, style = MaterialTheme.talkTypography.body, color = colors.accent)
        }
        Row(
            Modifier.clickable(role = Role.Button) {
                clipboard.setText(AnnotatedString(model.shareLink))
                Toast.makeText(context, linkCopied, Toast.LENGTH_SHORT).show()
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TalkIcon(TalkIcons.Copy, contentDescription = stringResource(MR.strings.post_copy_address), size = 18.dp, tint = colors.textSecondary)
            Text(model.shareLink.shortNpub(), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
        }
    }
    HorizontalDivider(color = colors.border, thickness = MaterialTheme.sizes.hairline)
}

/** "N replies" and the reply field (`t-reply-box`). */
@Composable
private fun RepliesHeader(model: PostComponent.Model, component: PostComponent, focus: FocusRequester) {
    val spacing = MaterialTheme.spacing
    Column(Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(pluralResource(MR.plurals.replies_count, model.replies.size), style = MaterialTheme.talkTypography.headingMedium, color = MaterialTheme.talkColors.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            OutlinedTextField(
                value = model.replyText,
                onValueChange = component::onReplyTextChanged,
                placeholder = { Text(stringResource(MR.strings.post_write_reply)) },
                modifier = Modifier.weight(1f).focusRequester(focus),
                shape = MaterialTheme.talkShapes.pill,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (model.canSendReply) component.onSendReply() }),
            )
            TalkButton(stringResource(MR.strings.action_reply), component::onSendReply, ButtonStyle.Post, enabled = model.canSendReply && !model.isReplying)
        }
    }
    HorizontalDivider(color = MaterialTheme.talkColors.border, thickness = MaterialTheme.sizes.hairline)
}

@Composable
private fun ReplyCard(reply: ThreadReply, component: PostComponent) {
    val post = reply.post
    var revealed by rememberSaveable(post.id.value) { mutableStateOf(false) }
    if (LocalModeration.current.hiddenReason(post.id, post.author.id) != null && !revealed) {
        Text(
            stringResource(MR.strings.moderation_hidden),
            style = MaterialTheme.talkTypography.body,
            color = MaterialTheme.talkColors.textSecondary,
            modifier = Modifier.fillMaxWidth().clickable { revealed = true }.padding(MaterialTheme.spacing.lg),
        )
        return
    }
    val spacing = MaterialTheme.spacing
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { component.onOpenPost(post.id) }) {
        Row(
            // Deeper replies step in, up to a few levels.
            Modifier.padding(start = spacing.lg + (minOf(reply.depth, MAX_INDENT) * 16).dp, end = spacing.lg, top = spacing.md, bottom = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Avatar(post.author, onClick = { component.onOpenAuthor(post.author.id) })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PostHead(post, onEdited = null)
                TalkMarkdown(post.content)
                PostActions(post, PostActionHandlers(component::onReplyInEditor, component::onRepost, component::onLike, component::onBookmark, {}))
            }
        }
        HorizontalDivider(color = MaterialTheme.talkColors.border, thickness = MaterialTheme.sizes.hairline)
    }
}

/** More (a menu): Quote, Copy link; Edit and Delete on your own post, Report on others'. */
@Composable
private fun MoreActions(post: Post, model: PostComponent.Model, component: PostComponent, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val moderation = LocalModerationActions.current
    val linkCopied = stringResource(MR.strings.toast_link_copied)
    val reported = stringResource(MR.strings.toast_reported)
    Box {
        GlassIconButton(TalkIcons.More, stringResource(MR.strings.action_more), { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(MR.strings.action_quote)) }, onClick = { open = false; component.onQuote(post) })
            DropdownMenuItem(text = { Text(stringResource(MR.strings.post_copy_link)) }, onClick = {
                open = false
                clipboard.setText(AnnotatedString(model.shareLink))
                Toast.makeText(context, linkCopied, Toast.LENGTH_SHORT).show()
            })
            if (model.isMine) {
                DropdownMenuItem(text = { Text(stringResource(MR.strings.action_edit)) }, onClick = { open = false; component.onEdit() })
                DropdownMenuItem(text = { Text(stringResource(MR.strings.action_delete), color = MaterialTheme.talkColors.error) }, onClick = { open = false; onDelete() })
            } else {
                DropdownMenuItem(text = { Text(stringResource(MR.strings.action_report)) }, onClick = { open = false; reporting = true })
            }
        }
        // Report: the public-report note, then the reasons (NIP-56).
        DropdownMenu(expanded = reporting, onDismissRequest = { reporting = false }) {
            Text(
                stringResource(MR.strings.report_note),
                style = MaterialTheme.talkTypography.small,
                color = MaterialTheme.talkColors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(0.8f),
            )
            ReportReason.entries.forEach { reason ->
                DropdownMenuItem(text = { Text(stringResource(reason.label)) }, onClick = {
                    reporting = false
                    moderation?.report(ReportTarget.Post(post.id, post.author.id), reason)
                    Toast.makeText(context, reported, Toast.LENGTH_SHORT).show()
                })
            }
        }
    }
}

private fun share(context: android.content.Context, link: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, link)
    context.startActivity(Intent.createChooser(send, null))
}

/** A leading `# Title` line, and the markdown after it; no title when the post does not start with one. */
private fun splitTitle(content: String): Pair<String?, String> {
    val trimmed = content.trimStart()
    val first = trimmed.lineSequence().firstOrNull().orEmpty()
    return if (first.startsWith("# ")) first.removePrefix("# ").trim() to trimmed.removePrefix(first).trimStart('\n') else null to content
}

private val ReportReason.label
    get() = when (this) {
        ReportReason.Nudity -> MR.strings.report_nudity
        ReportReason.Spam -> MR.strings.report_spam
        ReportReason.Illegal -> MR.strings.report_illegal
        ReportReason.Impersonation -> MR.strings.report_impersonation
        ReportReason.Other -> MR.strings.report_other
    }

private const val MAX_INDENT = 4
