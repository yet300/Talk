package com.yet.talk.ui.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.chat.component.conversation.ConversationComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.ConfirmDialog
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.LocalNetwork
import com.yet.talk.ui.components.Notice
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.TalkIconButton
import com.yet.talk.ui.components.TalkTextField
import com.yet.talk.ui.components.glass.GlassIconButton
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.components.glass.GlassTopBar
import com.yet.talk.ui.platform.SecureScreen
import com.yet.talk.ui.platform.copyText
import com.yet.talk.ui.platform.rememberImagePicker
import com.yet.talk.ui.screens.chat.component.ChatAvatar
import com.yet.talk.ui.screens.chat.component.ImageViewer
import com.yet.talk.ui.screens.chat.component.MessageBubble
import com.yet.talk.ui.screens.chat.component.MessageRow
import com.yet.talk.ui.screens.chat.component.chatTitle
import com.yet.talk.ui.screens.chat.component.dayLabel
import com.yet.talk.ui.screens.chat.component.messageRows
import com.yet.talk.ui.screens.chat.sheet.MessageActions
import com.yet.talk.ui.screens.chat.sheet.MessageSheet
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A conversation (`Conversation`, `Chat-Mobile`): the glass bar with the person or group (their info behind it), the
 * messages from the bottom up, and the composer with the reply bar and Add image, or the request bar (Accept, Delete,
 * Block) until a request is accepted. Back cancels a reply first. No read receipts, no typing indicator.
 */
@Composable
fun ConversationContent(component: ConversationComponent) {
    SecureScreen()
    val model by component.model.subscribeAsState()
    val title = chatTitle(model.name, model.members)
    val context = LocalContext.current
    val copied = stringResource(MR.strings.action_copied)
    val input = remember { FocusRequester() }
    var actionsFor by rememberSaveable { mutableStateOf<String?>(null) }
    var viewing by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = model.replyingTo != null) { component.onReply(null) }
    val zone = remember { TimeZone.currentSystemDefault() }
    val rows = remember(model.messages) { messageRows(model.messages, zone).asReversed() }
    val today = remember(model.messages) { Clock.System.now().toLocalDateTime(zone).date }
    val names = remember(model.members) { model.members.associate { it.id to it.displayLabel } }
    val you = stringResource(MR.strings.chats_you, "").trimEnd(' ', ':')
    val list = rememberLazyListState()
    // Keep the newest message in view as messages arrive (the list is drawn bottom up).
    LaunchedEffect(model.messages.lastOrNull()?.id) { if (list.firstVisibleItemIndex <= 1) list.animateScrollToItem(0) }
    val actions = remember(component) {
        MessageActions(
            onReply = { component.onReply(it); input.requestFocus() },
            onReact = component::onReact,
            onCopy = { copyText(context, it.content, copied) },
            onDeleteForEveryone = component::onDeleteForEveryone,
            onDeleteForMe = component::onDeleteForMe,
        )
    }
    GlassPage(
        modifier = Modifier.imePadding(),
        bar = {
            GlassTopBar(
                leading = { GlassIconButton(TalkIcons.Back, stringResource(MR.strings.action_back), component::onBack) },
                title = {
                    val info = stringResource(MR.strings.chat_info)
                    Row(
                        Modifier.weight(1f).clickable(role = Role.Button, onClick = component::onOpenInfo).semantics(mergeDescendants = true) { contentDescription = "$title, $info" },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        ChatAvatar(model.members, size = 32.dp)
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(title, style = MaterialTheme.talkTypography.bodyBold, color = MaterialTheme.talkColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                if (model.isVerified) TalkIcon(TalkIcons.Check, null, size = 16.dp, tint = MaterialTheme.talkColors.accent)
                            }
                            Text(
                                when {
                                    model.isGroup -> stringResource(MR.strings.chat_you_and, model.members.joinToString(", ") { it.displayLabel })
                                    model.disappearing != null -> stringResource(MR.strings.chat_disappearing_on)
                                    else -> model.members.firstOrNull()?.handle.orEmpty()
                                },
                                style = MaterialTheme.talkTypography.small, color = MaterialTheme.talkColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                trailing = { GlassIconButton(TalkIcons.Info, stringResource(MR.strings.chat_info), component::onOpenInfo) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = list,
                reverseLayout = true,
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + MaterialTheme.spacing.sm, bottom = MaterialTheme.spacing.sm),
            ) {
                items(rows, key = { it.key }, contentType = { it::class }) { row ->
                    when (row) {
                        is MessageRow.Day -> Text(
                            dayLabel(row.date, today),
                            style = MaterialTheme.talkTypography.small,
                            color = MaterialTheme.talkColors.textSecondary,
                            modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.md).animateItem(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        is MessageRow.Message -> MessageBubble(
                            row = row,
                            isGroup = model.isGroup,
                            names = names,
                            you = you,
                            image = model.images[row.message.id],
                            onOpenActions = { if (!row.message.isExpired) actionsFor = row.message.id },
                            onShowImage = { component.onShowImage(row.message) },
                            onOpenImage = { viewing = row.message.id },
                            onRetry = { component.onRetry(row.message) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                item(key = "error") { ErrorRow(model.error, onDismiss = component::onErrorShown) }
                item(key = "notice") {
                    when {
                        LocalNetwork.current.isOffline -> Notice(TalkIcons.Offline, stringResource(MR.strings.chat_offline))
                        model.canReceive == false -> Notice(TalkIcons.Info, stringResource(MR.strings.chat_member_cannot_receive, title))
                    }
                }
            }
            if (model.isRequest) RequestBar(title, component) else Composer(model, component, input)
        }
    }
    val message = model.messages.firstOrNull { it.id == actionsFor }
    if (message != null) MessageSheet(message, model.reactions, actions, onDismiss = { actionsFor = null })
    val image = viewing?.let { model.images[it] as? ConversationComponent.ImageState.Loaded }
    if (image != null) ImageViewer(image.bytes, onClose = { viewing = null })
}

@Composable
private fun Composer(model: ConversationComponent.Model, component: ConversationComponent, input: FocusRequester) {
    val off = model.canReceive == false
    val colors = MaterialTheme.talkColors
    val pick = rememberImagePicker(component::onSendImage)
    Column(Modifier.fillMaxWidth().background(colors.bg).navigationBarsPadding()) {
        AnimatedVisibility(model.replyingTo != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            val reply = model.replyingTo
            if (reply != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                    TalkIcon(TalkIcons.Reply, null, size = 18.dp, tint = colors.accent)
                    Column(Modifier.weight(1f)) {
                        val who = if (reply.isMine) stringResource(MR.strings.chats_you, "").trimEnd(' ', ':') else model.members.firstOrNull { it.id == reply.author }?.displayLabel.orEmpty()
                        Text(stringResource(MR.strings.chat_replying_to, who), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
                        Text(reply.content.ifEmpty { stringResource(MR.strings.chat_photo) }, style = MaterialTheme.talkTypography.body, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TalkIconButton(TalkIcons.Close, stringResource(MR.strings.chat_cancel_reply), { component.onReply(null) }, size = 18.dp)
                }
            }
        }
        AnimatedVisibility(model.isUploading) {
            Row(Modifier.padding(horizontal = MaterialTheme.spacing.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                CircularProgressIndicator(Modifier.padding(4.dp).size(16.dp), strokeWidth = 2.dp, color = colors.accent)
                Text(stringResource(MR.strings.chat_encrypting), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            TalkIconButton(TalkIcons.Image, stringResource(MR.strings.chat_add_image), pick, enabled = !off && !model.isUploading)
            TalkTextField(
                value = model.text,
                onValueChange = component::onTextChanged,
                placeholder = stringResource(MR.strings.chat_message),
                modifier = Modifier.weight(1f).focusRequester(input),
                pill = true,
                singleLine = false,
                enabled = !off,
                imeAction = ImeAction.Send,
                onDone = { if (model.canSend) component.onSend() },
            )
            TalkIconButton(TalkIcons.Send, stringResource(MR.strings.chat_send), component::onSend, enabled = model.canSend, tint = colors.accent)
        }
    }
}

@Composable
private fun RequestBar(name: String, component: ConversationComponent) {
    var blocking by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.talkColors.bg).navigationBarsPadding().padding(MaterialTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Text(stringResource(MR.strings.chat_request_note, name), style = MaterialTheme.talkTypography.body, color = MaterialTheme.talkColors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            TalkButton(stringResource(MR.strings.action_accept), component::onAccept, ButtonStyle.Primary)
            TalkButton(stringResource(MR.strings.action_delete), component::onDelete, ButtonStyle.Secondary)
            TalkButton(stringResource(MR.strings.action_block), { blocking = true }, ButtonStyle.Secondary)
        }
    }
    if (blocking) {
        ConfirmDialog(
            title = stringResource(MR.strings.chat_block_title, name),
            body = stringResource(MR.strings.chat_block_body),
            confirm = stringResource(MR.strings.action_block),
            onConfirm = { blocking = false; component.onBlock() },
            onDismiss = { blocking = false },
        )
    }
}
