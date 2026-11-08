package com.yet.talk.ui.screens.chat.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.resources.*
import com.yet.talk.ui.components.SheetRow
import com.yet.talk.ui.components.TalkBottomSheet
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors

/** What a message can do; the conversation implements these. */
class MessageActions(
    val onReply: (ChatMessage) -> Unit,
    val onReact: (ChatMessage, String) -> Unit,
    val onCopy: (ChatMessage) -> Unit,
    val onDeleteForEveryone: (ChatMessage) -> Unit,
    val onDeleteForMe: (ChatMessage) -> Unit,
)

/**
 * The message sheet (`Conversation`, menu on a phone): the reactions, Reply, Copy, Delete for everyone (yours) with
 * "Other apps may still show it", and Delete for me.
 */
@Composable
fun MessageSheet(message: ChatMessage, reactions: List<String>, actions: MessageActions, onDismiss: () -> Unit) {
    TalkBottomSheet(onDismiss = onDismiss) { close ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.xl, vertical = MaterialTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            reactions.forEach { emoji ->
                val label = stringResource(MR.strings.chat_react, emoji)
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.talkColors.surface)
                        .clickable(role = Role.Button) { close { actions.onReact(message, emoji) } }
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) { Text(emoji, fontSize = 22.sp) }
            }
        }
        SheetRow(stringResource(MR.strings.chat_reply), icon = TalkIcons.Reply, onClick = { close { actions.onReply(message) } })
        if (message.content.isNotEmpty()) SheetRow(stringResource(MR.strings.action_copy), icon = TalkIcons.Copy, onClick = { close { actions.onCopy(message) } })
        if (message.isMine) {
            SheetRow(
                stringResource(MR.strings.chat_delete_everyone), icon = TalkIcons.Trash, danger = true,
                note = stringResource(MR.strings.chat_delete_everyone_note),
                onClick = { close { actions.onDeleteForEveryone(message) } },
            )
        }
        SheetRow(stringResource(MR.strings.chat_delete_me), icon = TalkIcons.Trash, danger = true, onClick = { close { actions.onDeleteForMe(message) } })
    }
}
