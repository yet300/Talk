package com.yet.talk.web.ui.screens.chat.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.user.User
import com.yet.talk.feature.chat.component.conversation.ConversationComponent.ImageState
import com.yet.talk.resources.*
import com.yet.talk.web.browser.rememberBlobUrl
import com.yet.talk.web.browser.rememberIsPhone
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.post.ImageViewer
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import kotlin.time.Clock

/** What a message can do; the conversation implements these. */
class MessageActions(
    val onRetry: (ChatMessage) -> Unit,
    val onReply: (ChatMessage) -> Unit,
    val onReact: (ChatMessage, String) -> Unit,
    val onCopy: (ChatMessage) -> Unit,
    val onDeleteForEveryone: (ChatMessage) -> Unit,
    val onDeleteForMe: (ChatMessage) -> Unit,
    val onShowImage: (ChatMessage) -> Unit,
)

/**
 * The bubbles (`t-msgs`): yours on the right in `bubble-out`, theirs on the left, grouped by sender (a gap before a
 * new sender, the tail on the last of a run) and by day with a divider; sender names in groups; reply quotes,
 * images and reactions inside. A message opens its menu from the … button (hover on the web) or a tap on a phone.
 */
@Composable
fun Messages(
    messages: List<ChatMessage>,
    isGroup: Boolean,
    members: List<User>,
    images: Map<String, ImageState>,
    reactions: List<String>,
    actions: MessageActions,
) {
    val zone = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(zone).date
    val names = members.associate { it.id to it.displayLabel }
    val you = stringResource(MR.strings.chats_you, "").trimEnd(' ', ':')
    var menuFor by remember { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf<String?>(null) }
    val phone = rememberIsPhone()
    Div({ classes("t-msgs") }) {
        messages.forEachIndexed { i, message ->
            val day = message.createdAt.toLocalDateTime(zone).date
            val prev = messages.getOrNull(i - 1)
            val next = messages.getOrNull(i + 1)
            val newDay = prev == null || prev.createdAt.toLocalDateTime(zone).date != day
            if (newDay) Div({ classes("t-divider") }) { Text(dayLabel(day, today)) }
            val samePrev = !newDay && prev?.author == message.author
            val sameNext = next != null && next.author == message.author && next.createdAt.toLocalDateTime(zone).date == day
            Div({
                classes("t-msg")
                if (message.isMine) classes("t-msg-mine")
                if (!samePrev) classes("t-msg-gap")
                if (!sameNext) classes("t-msg-tail")
            }) {
                if (isGroup && !message.isMine && !samePrev) Span({ classes("t-sender") }) { Text(names[message.author].orEmpty()) }
                val canAct = !message.isExpired
                Div({ classes("t-msg-row") }) {
                    Bubble(message, images[message.id], names, you, onOpenImage = { viewing = message.id }, onShowImage = { actions.onShowImage(message) }) {
                        if (phone && canAct) menuFor = message.id
                    }
                    if (!phone && canAct) MoreButton { menuFor = message.id }
                }
                if (message.reactions.isNotEmpty()) {
                    Div({ classes("t-reacts") }) {
                        message.reactions.forEach { reaction ->
                            Span({ classes("t-react"); if (reaction.isMine) classes("t-react-mine") }) {
                                Text(if (reaction.count > 1) "${reaction.emoji} ${reaction.count}" else reaction.emoji)
                            }
                        }
                    }
                }
                when (message.publishState) {
                    PublishState.Pending -> Span({ classes("t-msg-meta"); attr("role", "status") }) { Text(stringResource(MR.strings.post_pending)) }
                    PublishState.Failed -> Span({ classes("t-msg-meta"); attr("role", "alert") }) {
                        Span({ classes("t-send-fail") }) { Text(stringResource(MR.strings.post_failed)) }
                        TalkButton(stringResource(MR.strings.action_retry), { actions.onRetry(message) }, ButtonStyle.Secondary)
                    }
                    else -> Unit
                }
                if (menuFor == message.id) MessageMenu(message, reactions, sheet = phone, actions = actions, onDismiss = { menuFor = null })
            }
        }
    }
    viewing?.let { id ->
        val url = (images[id] as? ImageState.Loaded)?.let { rememberBlobUrl(it.bytes, messages.firstOrNull { m -> m.id == id }?.image?.mimeType) }
        if (url != null) ImageViewer(listOf(Image(url)), 0, onClose = { viewing = null }) else viewing = null
    }
}

@Composable
private fun Bubble(
    message: ChatMessage,
    image: ImageState?,
    names: Map<com.yet.talk.domain.model.user.UserId, String>,
    you: String,
    onOpenImage: () -> Unit,
    onShowImage: () -> Unit,
    onTap: () -> Unit,
) {
    if (message.isExpired) {
        Div({ classes("t-bubble", "t-expired") }) { Text(stringResource(MR.strings.chat_expired)) }
        return
    }
    if (message.image != null) {
        LaunchedEffect(message.id) { onShowImage() }
        val label = stringResource(MR.strings.chat_open_image)
        when (image) {
            is ImageState.Loaded -> Button({
                classes("t-msg-img")
                type(ButtonType.Button)
                attr("aria-label", label)
                onClick { onOpenImage() }
            }) { Img(src = rememberBlobUrl(image.bytes, message.image?.mimeType), alt = "") }
            ImageState.Failed -> Div({ classes("t-msg-img") }) { Span({ classes("t-caption") }) { Text(stringResource(MR.strings.chat_image_failed)) } }
            else -> Div({ classes("t-msg-img", "x-skel"); attr("aria-busy", "true") }) { TalkIcon(TalkIcons.Image) }
        }
        return
    }
    Div({
        classes("t-bubble")
        onClick { onTap() }
    }) {
        message.replyTo?.let { quote ->
            Span({ classes("t-quote-in") }) {
                if (quote.text != null || quote.isImage) {
                    Span({ classes("t-quote-who") }) { Text(if (quote.author == message.author && message.isMine) you else names[quote.author] ?: you) }
                    Text(quote.text ?: stringResource(MR.strings.chat_photo))
                } else {
                    Text(stringResource(MR.strings.chat_reply_unavailable))
                }
            }
        }
        Text(message.content)
    }
}

@Composable
private fun MoreButton(onClick: () -> Unit) {
    val label = stringResource(MR.strings.chat_message_actions)
    Button({
        classes("t-icon-btn", "t-msg-more")
        type(ButtonType.Button)
        attr("aria-label", label)
        attr("aria-haspopup", "menu")
        onClick { it.stopPropagation(); onClick() }
    }) { TalkIcon(TalkIcons.More, size = 18f) }
}

/**
 * The message menu (`Conversation`, menu): reactions, Reply, Copy, Delete for everyone (yours, sent) and Delete for
 * me. A bottom sheet on a phone, a menu under the bubble on the web.
 */
@Composable
private fun MessageMenu(message: ChatMessage, reactions: List<String>, sheet: Boolean, actions: MessageActions, onDismiss: () -> Unit) {
    val title = stringResource(MR.strings.chat_message_actions)
    val body: @Composable () -> Unit = {
        Div({ classes("t-react-row") }) {
            reactions.forEach { emoji ->
                val label = stringResource(MR.strings.chat_react, emoji)
                Button({
                    classes("t-react-btn")
                    type(ButtonType.Button)
                    attr("aria-label", label)
                    onClick { it.stopPropagation(); onDismiss(); actions.onReact(message, emoji) }
                }) { Text(emoji) }
            }
        }
        MenuRow(TalkIcons.Reply, stringResource(MR.strings.chat_reply)) { onDismiss(); actions.onReply(message) }
        if (message.content.isNotEmpty()) MenuRow(TalkIcons.Copy, stringResource(MR.strings.action_copy)) { onDismiss(); actions.onCopy(message) }
        if (message.isMine) {
            MenuRow(TalkIcons.Trash, stringResource(MR.strings.chat_delete_everyone), note = stringResource(MR.strings.chat_delete_everyone_note), danger = true) {
                onDismiss()
                actions.onDeleteForEveryone(message)
            }
        }
        MenuRow(TalkIcons.Trash, stringResource(MR.strings.chat_delete_me), danger = true) { onDismiss(); actions.onDeleteForMe(message) }
    }
    if (sheet) {
        Div({ classes("t-layer", "x-scrim"); onClick { onDismiss() } }) {
            Div({
                classes("t-bs")
                attr("role", "dialog")
                attr("aria-modal", "true")
                attr("aria-label", title)
                onClick { it.stopPropagation() }
            }) {
                body()
                TalkButton(stringResource(MR.strings.action_cancel), onDismiss, ButtonStyle.Secondary, attrs = { classes("t-btn-44", "t-bs-cancel") })
            }
        }
    } else {
        Div({ classes("t-menu-backdrop"); onClick { it.stopPropagation(); onDismiss() } })
        Div({ classes("x-menu", "t-msg-menu"); attr("role", "menu"); attr("aria-label", title) }) { body() }
    }
}

@Composable
private fun MenuRow(icon: dev.icerock.moko.resources.ImageResource, label: String, note: String? = null, danger: Boolean = false, onClick: () -> Unit) {
    Button({
        classes("t-menu-row")
        if (danger) classes("t-danger")
        type(ButtonType.Button)
        attr("role", "menuitem")
        onClick { it.stopPropagation(); onClick() }
    }) {
        TalkIcon(icon, size = 18f)
        if (note == null) {
            Text(label)
        } else {
            Span {
                Text(label)
                Span({ classes("t-menu-sub") }) { Text(note) }
            }
        }
    }
}

@Composable
private fun dayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> stringResource(MR.strings.chat_today)
    today.minus(DatePeriod(days = 1)) -> stringResource(MR.strings.chat_yesterday)
    else -> if (day.year == today.year) "${day.day} ${monthName(day)}" else "${day.day} ${monthName(day)} ${day.year}"
}

private fun monthName(day: LocalDate): String =
    kotlin.js.Date(day.year, day.month.ordinal, day.day).asDynamic().toLocaleDateString(undefined, js("({month:'short'})")) as String
