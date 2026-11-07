package com.yet.talk.web.ui.screens.chat.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.user.User
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.theme.stringResource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.DatePeriod
import kotlin.time.Clock
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * The bubbles (`t-msgs`): yours on the right in `bubble-out`, theirs on the left, grouped by sender (a gap
 * before a new sender, the tail on the last of a run) and by day with a divider; sender names in groups.
 */
@Composable
fun Messages(messages: List<ChatMessage>, isGroup: Boolean, members: List<User>, onRetry: (ChatMessage) -> Unit) {
    val zone = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(zone).date
    val names = members.associate { it.id to it.displayLabel }
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
                Div({ classes("t-bubble") }) { Text(message.content) }
                when (message.publishState) {
                    PublishState.Pending -> Span({ classes("t-msg-meta"); attr("role", "status") }) { Text(stringResource(MR.strings.post_pending)) }
                    PublishState.Failed -> Span({ classes("t-msg-meta"); attr("role", "alert") }) {
                        Span({ classes("t-send-fail") }) { Text(stringResource(MR.strings.post_failed)) }
                        TalkButton(stringResource(MR.strings.action_retry), { onRetry(message) }, ButtonStyle.Secondary)
                    }
                    else -> Unit
                }
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
