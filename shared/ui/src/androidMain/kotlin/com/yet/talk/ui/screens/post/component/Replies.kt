package com.yet.talk.ui.screens.post.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.domain.model.post.ThreadReply
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.Avatar
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.LocalModeration
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.TalkMarkdown
import com.yet.talk.ui.components.TalkTextField
import com.yet.talk.ui.components.post.PostActionHandlers
import com.yet.talk.ui.components.post.PostActions
import com.yet.talk.ui.components.post.PostHead
import com.yet.talk.ui.theme.pluralResource
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/** "N replies" and the reply field (`t-reply-box`); Reply stays off until there is text. */
@Composable
fun RepliesHeader(model: PostComponent.Model, component: PostComponent, focus: FocusRequester) {
    val spacing = MaterialTheme.spacing
    Column(Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(pluralResource(MR.plurals.replies_count, model.replies.size), style = MaterialTheme.talkTypography.headingMedium, color = MaterialTheme.talkColors.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            TalkTextField(
                value = model.replyText,
                onValueChange = component::onReplyTextChanged,
                placeholder = stringResource(MR.strings.post_write_reply),
                modifier = Modifier.weight(1f).focusRequester(focus),
                pill = true,
                imeAction = ImeAction.Send,
                onDone = { if (model.canSendReply) component.onSendReply() },
            )
            TalkButton(stringResource(MR.strings.action_reply), component::onSendReply, ButtonStyle.Post, enabled = model.canSendReply && !model.isReplying)
        }
    }
    HorizontalDivider(color = MaterialTheme.talkColors.border, thickness = MaterialTheme.sizes.hairline)
}

/** A reply card: avatar, head, the markdown, Reply (opens the editor) and Like; deeper replies step in. */
@Composable
fun ReplyCard(reply: ThreadReply, component: PostComponent, modifier: Modifier = Modifier) {
    val post = reply.post
    var revealed by rememberSaveable(post.id.value) { mutableStateOf(false) }
    if (LocalModeration.current.hiddenReason(post.id, post.author.id) != null && !revealed) {
        Text(
            stringResource(MR.strings.moderation_hidden),
            style = MaterialTheme.talkTypography.body,
            color = MaterialTheme.talkColors.textSecondary,
            modifier = modifier.fillMaxWidth().clickable { revealed = true }.padding(MaterialTheme.spacing.lg),
        )
        return
    }
    val spacing = MaterialTheme.spacing
    val actions = remember(component) { PostActionHandlers(component::onReplyInEditor, component::onRepost, component::onLike, component::onBookmark, {}) }
    Column(modifier.fillMaxWidth().clickable(role = Role.Button) { component.onOpenPost(post.id) }) {
        Row(
            Modifier.padding(start = spacing.lg + (minOf(reply.depth, MAX_INDENT) * 16).dp, end = spacing.lg, top = spacing.md, bottom = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Avatar(post.author, onClick = { component.onOpenAuthor(post.author.id) })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PostHead(post, onEdited = null)
                TalkMarkdown(post.content)
                PostActions(post, actions)
            }
        }
        HorizontalDivider(color = MaterialTheme.talkColors.border, thickness = MaterialTheme.sizes.hairline)
    }
}

/** The replies alone, for the left pane in two panes (Duo inner: replies | article). */
@Composable
fun RepliesList(component: PostComponent, padding: PaddingValues) {
    val model by component.model.subscribeAsState()
    val focus = remember { FocusRequester() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item(key = "header") { RepliesHeader(model, component, focus) }
        items(model.replies, key = { it.post.id.value }, contentType = { "reply" }) { ReplyCard(it, component, Modifier.animateItem()) }
    }
}

private const val MAX_INDENT = 4
