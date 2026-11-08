package com.yet.talk.ui.screens.tab.chats.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ListRow
import com.yet.talk.ui.components.RowIcon
import com.yet.talk.ui.screens.chat.component.ChatAvatar
import com.yet.talk.ui.screens.chat.component.chatTitle
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import com.yet.talk.ui.theme.text

/** A chat in the list (`ChatList` row): avatar, name with verified / disappearing / muted marks, last message, time, the unread dot. */
@Composable
fun ChatRow(chat: Chat, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
    val colors = MaterialTheme.talkColors
    val label = listOf(if (unread) stringResource(MR.strings.chats_unread, name) else name, preview).joinToString(", ")
    ListRow(
        title = name,
        caption = preview,
        captionEmphasis = unread,
        selected = selected,
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = label },
        leading = { ChatAvatar(chat.members) },
        titleTrailing = {
            if (chat.isVerified) TalkIcon(TalkIcons.Check, stringResource(MR.strings.chat_verified), size = 16.dp, tint = colors.accent)
            if (chat.disappearing != null) TalkIcon(TalkIcons.Timer, stringResource(MR.strings.chat_disappearing_on), size = 14.dp, tint = colors.textSecondary)
            if (chat.isMuted) TalkIcon(TalkIcons.Mute, stringResource(MR.strings.chat_muted), size = 14.dp, tint = colors.textSecondary)
        },
        trailing = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(chat.lastAt.toRelativeDesc().text(), style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
                if (unread) Box(Modifier.size(10.dp).clip(CircleShape).background(colors.accent))
            }
        },
    )
}

/** The Location channels entry at the top of the Chats list. */
@Composable
fun ChannelsRow(selected: Boolean, onClick: () -> Unit) {
    ListRow(
        title = stringResource(MR.strings.channels_title),
        caption = stringResource(MR.strings.channels_row_caption),
        selected = selected,
        onClick = onClick,
        leading = { RowIcon(TalkIcons.Pin) },
        trailing = { TalkIcon(TalkIcons.Chevron, contentDescription = null, size = 18.dp, tint = MaterialTheme.talkColors.textSecondary) },
    )
}
