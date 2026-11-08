package com.yet.talk.ui.screens.chat.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.feature.chat.component.conversation.ConversationComponent.ImageState
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** One row of the conversation: a day divider, or a message with where it sits in its sender's run. */
sealed interface MessageRow {
    val key: String

    data class Day(val date: LocalDate) : MessageRow {
        override val key get() = "day-$date"
    }

    data class Message(val message: ChatMessage, val firstOfRun: Boolean, val lastOfRun: Boolean) : MessageRow {
        override val key get() = message.id
    }
}

/** Groups messages by day and by sender run (a gap before a new sender, the tail on the last of a run). */
fun messageRows(messages: List<ChatMessage>, zone: TimeZone): List<MessageRow> = buildList {
    messages.forEachIndexed { i, message ->
        val day = message.createdAt.toLocalDateTime(zone).date
        val prev = messages.getOrNull(i - 1)
        val next = messages.getOrNull(i + 1)
        val newDay = prev == null || prev.createdAt.toLocalDateTime(zone).date != day
        if (newDay) add(MessageRow.Day(day))
        val samePrev = !newDay && prev?.author == message.author
        val sameNext = next != null && next.author == message.author && next.createdAt.toLocalDateTime(zone).date == day
        add(MessageRow.Message(message, firstOfRun = !samePrev, lastOfRun = !sameNext))
    }
}

/**
 * A message (`t-msg`): yours on the right in `bubble-out` (decision 22), theirs on the left; the sender's name in
 * groups; reply quote, image and reactions inside; Sending… or Not sent with Retry under it. A tap or long press
 * opens the message sheet.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    row: MessageRow.Message,
    isGroup: Boolean,
    names: Map<UserId, String>,
    you: String,
    image: ImageState?,
    onOpenActions: () -> Unit,
    onShowImage: () -> Unit,
    onOpenImage: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = row.message
    val colors = MaterialTheme.talkColors
    val mine = message.isMine
    val corner = 18.dp
    val tail = 4.dp
    val shape = if (mine) RoundedCornerShape(corner, corner, if (row.lastOfRun) tail else corner, corner)
    else RoundedCornerShape(corner, corner, corner, if (row.lastOfRun) tail else corner)
    Column(
        modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.md).padding(top = if (row.firstOfRun) 10.dp else 2.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        if (isGroup && !mine && row.firstOfRun) {
            Text(names[message.author].orEmpty(), style = MaterialTheme.talkTypography.small, color = colors.textSecondary, modifier = Modifier.padding(start = 12.dp, bottom = 2.dp))
        }
        val actions = Modifier.combinedClickable(onClick = onOpenActions, onLongClick = onOpenActions)
        when {
            message.isExpired -> Box(Modifier.clip(shape).background(colors.surface).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(stringResource(MR.strings.chat_expired), style = MaterialTheme.talkTypography.body, color = colors.textSecondary)
            }
            message.image != null -> {
                LaunchedEffect(message.id) { onShowImage() }
                MessageImage(image, shape, onOpen = onOpenImage, onLongPress = onOpenActions)
            }
            else -> Column(
                Modifier.widthIn(max = 300.dp).clip(shape).background(if (mine) colors.bubbleOut else colors.bubbleTheirs).then(actions)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                message.replyTo?.let { quote ->
                    Column(Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.12f)).padding(8.dp)) {
                        if (quote.text != null || quote.isImage) {
                            Text(if (quote.author == message.author && mine) you else names[quote.author] ?: you, style = MaterialTheme.talkTypography.small, color = if (mine) Color.White else colors.textPrimary)
                            Text(quote.text ?: stringResource(MR.strings.chat_photo), style = MaterialTheme.talkTypography.small, color = if (mine) Color.White.copy(alpha = 0.85f) else colors.textSecondary, maxLines = 2)
                        } else {
                            Text(stringResource(MR.strings.chat_reply_unavailable), style = MaterialTheme.talkTypography.small, color = if (mine) Color.White else colors.textSecondary)
                        }
                    }
                }
                Text(message.content, style = MaterialTheme.talkTypography.body, color = if (mine) Color.White else colors.textPrimary)
            }
        }
        if (message.reactions.isNotEmpty()) {
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                message.reactions.forEach { reaction ->
                    Text(
                        if (reaction.count > 1) "${reaction.emoji} ${reaction.count}" else reaction.emoji,
                        style = MaterialTheme.talkTypography.small,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (reaction.isMine) colors.tintBlue else colors.surface).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
        when (message.publishState) {
            PublishState.Pending -> Text(stringResource(MR.strings.post_pending), style = MaterialTheme.talkTypography.tiny, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
            PublishState.Failed -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(MR.strings.post_failed), style = MaterialTheme.talkTypography.small, color = colors.error)
                TalkButton(stringResource(MR.strings.action_retry), onRetry, ButtonStyle.Secondary)
            }
            PublishState.Published -> Unit
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageImage(image: ImageState?, shape: RoundedCornerShape, onOpen: () -> Unit, onLongPress: () -> Unit) {
    val colors = MaterialTheme.talkColors
    val box = Modifier.size(220.dp).clip(shape).background(colors.surface)
    when (image) {
        is ImageState.Loaded -> {
            val bitmap = remember(image) { BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)?.asImageBitmap() }
            val label = stringResource(MR.strings.chat_open_image)
            if (bitmap != null) {
                Image(bitmap, contentDescription = label, contentScale = ContentScale.Crop, modifier = box.combinedClickable(role = Role.Image, onClick = onOpen, onLongClick = onLongPress))
            } else {
                Box(box, contentAlignment = Alignment.Center) { Text(stringResource(MR.strings.chat_image_failed), style = MaterialTheme.talkTypography.small, color = colors.textSecondary) }
            }
        }
        ImageState.Failed -> Box(box, contentAlignment = Alignment.Center) { Text(stringResource(MR.strings.chat_image_failed), style = MaterialTheme.talkTypography.small, color = colors.textSecondary) }
        else -> Box(box, contentAlignment = Alignment.Center) { TalkIcon(TalkIcons.Image, contentDescription = null, tint = colors.textSecondary) }
    }
}

/** The image viewer: the decrypted image full screen; back or a tap closes it. */
@Composable
fun ImageViewer(bytes: ByteArray, onClose: () -> Unit) {
    val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() } ?: return onClose()
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onClose).semantics { contentDescription = "" }, contentAlignment = Alignment.Center) {
            Image(bitmap, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
    }
}

/** "Today", "Yesterday", or the date. */
@Composable
fun dayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> stringResource(MR.strings.chat_today)
    today.minus(DatePeriod(days = 1)) -> stringResource(MR.strings.chat_yesterday)
    else -> java.time.LocalDate.of(day.year, day.month.ordinal + 1, day.day)
        .format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
}

