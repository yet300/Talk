package com.yet.talk.web.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.feature.chat.component.conversation.ConversationComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.copyToClipboard
import com.yet.talk.web.browser.rememberIsOnline
import com.yet.talk.web.browser.rememberIsPhone
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.AddImageButton
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.ConfirmDialog
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.IconButton
import com.yet.talk.web.ui.components.LocalToaster
import com.yet.talk.web.ui.components.Notice
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.screens.chat.component.ChatAvatar
import com.yet.talk.web.ui.screens.chat.component.MessageActions
import com.yet.talk.web.ui.screens.chat.component.Messages
import com.yet.talk.web.ui.screens.chat.component.chatTitle
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement

/**
 * A conversation (`Conversation`): the glass bar with the person or group (its info behind it), the messages
 * scrolling under it, and the composer with Reply and Add image, or the request bar (Accept, Delete, Block)
 * until a request is accepted. No read receipts, no typing indicator.
 */
@Composable
fun ConversationContent(component: ConversationComponent) {
    val model by component.model.subscribeAsState()
    val title = chatTitle(model.name, model.members)
    val toaster = LocalToaster.current
    val copied = stringResource(MR.strings.action_copied)
    Div({ classes("t-part", "t-convo") }) {
        Header({ classes("t-bar") }) {
            // Wider than a phone the list stays beside it, so Back is the phone's only.
            if (rememberIsPhone()) IconButton(TalkIcons.Back, stringResource(MR.strings.action_back), component::onBack)
            val info = stringResource(MR.strings.chat_info)
            A(href = "#", attrs = {
                classes("t-convo-who")
                attr("aria-label", "$title, $info")
                onClick { it.preventDefault(); component.onOpenInfo() }
            }) {
                ChatAvatar(model.members, small = true)
                Span({ classes("t-convo-who-text") }) {
                    Span({ classes("t-chat-row-name") }) {
                        Span({ classes("x-name", "t-ellipsis") }) { Text(title) }
                        if (model.isVerified) {
                            Span({ classes("t-on-blue-plain", "t-inline") }) { TalkIcon(TalkIcons.Check, stringResource(MR.strings.chat_verified), size = 18f) }
                        }
                    }
                    Span({ classes("t-caption", "t-ellipsis", "t-block") }) {
                        if (model.disappearing != null) TalkIcon(TalkIcons.Timer, size = 14f, attrs = { classes("t-inline-icon") })
                        Text(
                            when {
                                model.isGroup -> stringResource(MR.strings.chat_you_and, model.members.joinToString(", ") { it.displayLabel })
                                model.disappearing != null -> stringResource(MR.strings.chat_disappearing_on)
                                else -> model.members.firstOrNull()?.handle.orEmpty()
                            },
                        )
                    }
                }
            }
            IconButton(TalkIcons.Info, info, component::onOpenInfo)
        }
        var scroller by remember { mutableStateOf<HTMLElement?>(null) }
        Div({
            classes("t-convo-scroll")
            ref { scroller = it; onDispose { scroller = null } }
        }) {
            when {
                !rememberIsOnline() -> Notice(TalkIcons.Offline, stringResource(MR.strings.chat_offline))
                model.canReceive == false -> Notice(TalkIcons.Info, stringResource(MR.strings.chat_member_cannot_receive, title))
            }
            model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
            Messages(
                messages = model.messages,
                isGroup = model.isGroup,
                members = model.members,
                images = model.images,
                reactions = model.reactions,
                actions = MessageActions(
                    onRetry = component::onRetry,
                    onReply = {
                        component.onReply(it)
                        (document.getElementById("chat-input") as? HTMLElement)?.focus()
                    },
                    onReact = component::onReact,
                    onCopy = {
                        copyToClipboard(it.content)
                        toaster.show(copied)
                    },
                    onDeleteForEveryone = component::onDeleteForEveryone,
                    onDeleteForMe = component::onDeleteForMe,
                    onShowImage = component::onShowImage,
                ),
            )
        }
        // Keep the newest message in view as messages arrive.
        LaunchedEffect(model.messages.size, scroller) { scroller?.let { it.scrollTop = it.scrollHeight.toDouble() } }
        if (model.isRequest) RequestBar(title, component) else Composer(model, component)
    }
}

@Composable
private fun Composer(model: ConversationComponent.Model, component: ConversationComponent) {
    val off = model.canReceive == false
    val message = stringResource(MR.strings.chat_message)
    Div({ classes("t-chat-bar") }) {
        model.replyingTo?.let { reply ->
            Div({ classes("t-reply-bar") }) {
                TalkIcon(TalkIcons.Reply, size = 18f)
                Span({ classes("t-reply-text") }) {
                    val who = if (reply.isMine) stringResource(MR.strings.chats_you, "").trimEnd(' ', ':')
                    else model.members.firstOrNull { it.id == reply.author }?.displayLabel.orEmpty()
                    Span({ classes("t-caption", "t-block") }) { Text(stringResource(MR.strings.chat_replying_to, who)) }
                    Span({ classes("t-ellipsis", "t-block") }) { Text(reply.content.ifEmpty { stringResource(MR.strings.chat_photo) }) }
                }
                IconButton(TalkIcons.Close, stringResource(MR.strings.chat_cancel_reply), { component.onReply(null) }, size = 18f)
            }
        }
        if (model.isUploading) {
            Div({ classes("t-chat-attach"); attr("role", "status") }) {
                Span({ classes("t-strip-item", "x-skel") }) { TalkIcon(TalkIcons.Image) }
                Span({ classes("t-caption") }) { Text(stringResource(MR.strings.chat_encrypting)) }
            }
        }
        AddImageButton("chat-image", stringResource(MR.strings.chat_add_image), component::onSendImage, enabled = !off && !model.isUploading)
        Label(forId = "chat-input", attrs = { classes("t-sr") }) { Text(message) }
        Input(InputType.Text) {
            id("chat-input")
            classes("t-chat-input")
            attr("placeholder", message)
            attr("autocomplete", "off")
            if (off) disabled()
            value(model.text)
            onInput { component.onTextChanged(it.value) }
            onKeyDown {
                when {
                    it.key == "Enter" && !it.shiftKey && !it.isComposing -> {
                        it.preventDefault()
                        if (model.canSend) component.onSend()
                    }
                    it.key == "Escape" && model.replyingTo != null -> component.onReply(null)
                }
            }
        }
        IconButton(TalkIcons.Send, stringResource(MR.strings.chat_send), component::onSend, attrs = { if (!model.canSend) disabled() })
    }
}

@Composable
private fun RequestBar(name: String, component: ConversationComponent) {
    var blocking by remember { mutableStateOf(false) }
    Div({ classes("t-chat-bar", "t-request-bar") }) {
        P({ classes("t-flush", "t-request-note") }) { Text(stringResource(MR.strings.chat_request_note, name)) }
        Div({ classes("t-request-actions") }) {
            TalkButton(stringResource(MR.strings.action_accept), component::onAccept, ButtonStyle.Primary)
            TalkButton(stringResource(MR.strings.action_delete), component::onDelete, ButtonStyle.Secondary)
            TalkButton(stringResource(MR.strings.action_block), { blocking = true }, ButtonStyle.Secondary, attrs = { classes("t-danger") })
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
