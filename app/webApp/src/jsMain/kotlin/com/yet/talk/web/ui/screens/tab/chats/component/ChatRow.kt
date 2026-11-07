package com.yet.talk.web.ui.screens.tab.chats.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.theme.stringResource
import com.yet.talk.web.ui.theme.text
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** A chat in the list (`ChatList` row): avatar, name, last message, time, the unread dot. */
@Composable
fun ChatRow(chat: Chat, selected: Boolean, onClick: () -> Unit) {
    val name = chat.members.joinToString(", ") { it.displayLabel }
    val preview = chat.lastMessage?.let { message ->
        when {
            chat.lastState == PublishState.Failed -> stringResource(MR.strings.post_failed)
            chat.lastState != null -> stringResource(MR.strings.chats_you, message)
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
        // A group gets two stacked avatars.
        if (chat.members.size > 1) {
            Span({ classes("t-av-stack"); attr("aria-hidden", "true") }) {
                Avatar(chat.members[0])
                Avatar(chat.members[1])
            }
        } else {
            Avatar(chat.members.firstOrNull())
        }
        Span({ classes("t-row-text") }) {
            Span({ classes("x-name", "t-ellipsis", "t-block") }) { Text(name) }
            Span({ classes("t-ellipsis", "t-block", if (unread) "t-preview-unread" else "t-muted") }) { Text(preview) }
        }
        Span({ classes("t-chat-row-end") }) {
            Span({ classes("t-caption") }) { Text(chat.lastAt.toRelativeDesc().text()) }
            if (unread) Span({ classes("t-unread") })
        }
    }
}
