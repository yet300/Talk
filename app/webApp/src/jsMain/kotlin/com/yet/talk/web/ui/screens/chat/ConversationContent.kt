package com.yet.talk.web.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.feature.chat.component.conversation.ConversationComponent
import com.yet.talk.resources.*
import com.yet.talk.web.browser.rememberIsOnline
import com.yet.talk.web.browser.rememberIsPhone
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.components.AvatarSize
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.IconButton
import com.yet.talk.web.ui.components.Notice
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.screens.chat.component.Messages
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
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
 * A conversation (`Conversation`): the glass bar with the person, the messages scrolling under it, and the
 * composer, or the request bar (Accept, Delete) until a request is accepted. No read receipts, no typing.
 */
@Composable
fun ConversationContent(component: ConversationComponent) {
    val model by component.model.subscribeAsState()
    val name = model.members.joinToString(", ") { it.displayLabel }
    Div({ classes("t-part", "t-convo") }) {
        Header({ classes("t-bar") }) {
            // Wider than a phone the list stays beside it, so Back is the phone's only.
            if (rememberIsPhone()) IconButton(TalkIcons.Back, stringResource(MR.strings.action_back), component::onBack)
            val first = model.members.firstOrNull()
            A(href = "#", attrs = {
                classes("t-convo-who")
                onClick { it.preventDefault(); first?.let { user -> component.onOpenMember(user.id) } }
            }) {
                Avatar(first, AvatarSize.Small)
                Span({ classes("t-convo-who-text") }) {
                    Span({ classes("x-name", "t-ellipsis", "t-block") }) { Text(name) }
                    Span({ classes("t-caption", "t-ellipsis", "t-block") }) { Text(first?.handle.orEmpty()) }
                }
            }
        }
        var scroller by remember { mutableStateOf<HTMLElement?>(null) }
        Div({
            classes("t-convo-scroll")
            ref { scroller = it; onDispose { scroller = null } }
        }) {
            when {
                !rememberIsOnline() -> Notice(TalkIcons.Offline, stringResource(MR.strings.chat_offline))
                model.canReceive == false -> Notice(TalkIcons.Info, stringResource(MR.strings.chat_member_cannot_receive, name))
            }
            model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
            Messages(model.messages, isGroup = model.members.size > 1, members = model.members, onRetry = component::onRetry)
        }
        // Keep the newest message in view as messages arrive.
        LaunchedEffect(model.messages.size, scroller) { scroller?.let { it.scrollTop = it.scrollHeight.toDouble() } }
        if (model.isRequest) RequestBar(name, component) else Composer(model, component)
    }
}

@Composable
private fun Composer(model: ConversationComponent.Model, component: ConversationComponent) {
    val off = model.canReceive == false
    val message = stringResource(MR.strings.chat_message)
    Div({ classes("t-chat-bar") }) {
        Label(forId = "chat-input", attrs = { classes("t-sr") }) { Text(message) }
        Input(InputType.Text) {
            id("chat-input")
            classes("t-chat-input")
            attr("placeholder", message)
            attr("autocomplete", "off")
            if (off) disabled()
            value(model.text)
            onInput { component.onTextChanged(it.value) }
            onKeyDown { if (it.key == "Enter" && !it.shiftKey && !it.isComposing) { it.preventDefault(); if (model.canSend) component.onSend() } }
        }
        IconButton(TalkIcons.Send, stringResource(MR.strings.chat_send), component::onSend, attrs = { if (!model.canSend) disabled() })
    }
}

@Composable
private fun RequestBar(name: String, component: ConversationComponent) {
    Div({ classes("t-chat-bar", "t-request-bar") }) {
        P({ classes("t-flush", "t-request-note") }) { Text(stringResource(MR.strings.chat_request_note, name)) }
        Div({ classes("t-request-actions") }) {
            TalkButton(stringResource(MR.strings.action_accept), component::onAccept, ButtonStyle.Primary)
            TalkButton(stringResource(MR.strings.action_delete), component::onDelete, ButtonStyle.Secondary)
        }
    }
}
