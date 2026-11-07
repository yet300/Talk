package com.yet.talk.web.ui.screens.tab.chats.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.resources.*
import com.yet.talk.web.ui.screens.chat.component.ChatAvatar
import com.yet.talk.web.ui.screens.chat.component.chatTitle
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import com.yet.talk.web.ui.theme.text
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** A chat in the list (`ChatList` row): avatar, name, last message, time, the unread dot. */
@Composable
fun ChatRow(chat: Chat, selected: Boolean, onClick: () -> Unit) {
    val name = chatTitle(chat.name, chat.members)
    val last = if (chat.lastIsImage) stringResource(MR.strings.chat_photo) else chat.lastMessage
    val preview = last?.let { message ->
        when {
            chat.lastState == PublishState.Failed -> stringResource(MR.strings.post_failed)
            chat.lastIsMine -> stringResource(MR.strings.chats_you, message)
            else -> message
        }
    }.orEmpty()
    val unread = chat.unread > 0
    val label = listOf(if (unread) stringResource(MR.strings.chats_unread, name) else name, preview).joinToString(", ")
    A(href = "#", attrs = {
        classes("t-row")
        if (selected) {
            classes("t-selected")
            attr("aria-current", "true")
        }
        attr("aria-label", label)
        onClick { it.preventDefault(); onClick() }
    }) {
        ChatAvatar(chat.members)
        Span({ classes("t-row-text") }) {
            Span({ classes("t-chat-row-name") }) {
                Span({ classes("x-name", "t-ellipsis") }) { Text(name) }
                if (chat.isVerified) {
                    Span({ classes("t-on-blue-plain", "t-inline") }) { TalkIcon(TalkIcons.Check, stringResource(MR.strings.chat_verified), size = 18f) }
                }
                if (chat.disappearing != null) {
                    Span({ classes("t-muted", "t-inline") }) { TalkIcon(TalkIcons.Timer, stringResource(MR.strings.chat_disappearing_on), size = 16f) }
                }
                if (chat.isMuted) Span({ classes("t-muted", "t-inline") }) { TalkIcon(TalkIcons.Mute, stringResource(MR.strings.chat_muted), size = 16f) }
            }
            Span({ classes("t-ellipsis", "t-block", if (unread) "t-preview-unread" else "t-muted") }) { Text(preview) }
        }
        Span({ classes("t-chat-row-end") }) {
            Span({ classes("t-caption") }) { Text(chat.lastAt.toRelativeDesc().text()) }
            if (unread) Span({ classes("t-unread") })
        }
    }
}
