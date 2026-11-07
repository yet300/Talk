package com.yet.talk.web.ui.screens.channels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.domain.model.channel.ChannelLevel
import com.yet.talk.feature.channels.component.room.RoomComponent
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.IconButton
import com.yet.talk.web.ui.components.Notice
import com.yet.talk.web.ui.screens.channels.component.label
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.pluralResource
import com.yet.talk.web.ui.theme.stringResource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement

/**
 * A channel room (`ChannelRoom`): `#area · Level` with the people count, "public and not kept", a plain timeline of
 * `nick#abcd` lines with the teleport mark, and the composer. At Block level, decision 21's warning.
 */
@Composable
fun RoomContent(component: RoomComponent) {
    val model by component.model.subscribeAsState()
    val channel = model.channel
    val zone = TimeZone.currentSystemDefault()
    Div({ classes("t-part", "t-convo") }) {
        Header({ classes("t-bar") }) {
            IconButton(TalkIcons.Back, stringResource(MR.strings.action_back), component::onBack)
            Span({ classes("t-convo-who-text", "t-grow") }) {
                Span({ classes("x-name", "t-ellipsis", "t-block") }) {
                    Text(stringResource(MR.strings.channels_room_title, channel.geohash, stringResource(channel.level.label)))
                }
                Span({ classes("t-caption", "t-ellipsis", "t-block") }) {
                    val people = model.people?.let { pluralResource(MR.plurals.channels_people, it) } ?: stringResource(MR.strings.channels_people_unknown)
                    Text(if (channel.isTeleport) "$people · ${stringResource(MR.strings.channels_teleported_here)}" else people)
                }
            }
        }
        var scroller by remember { mutableStateOf<HTMLElement?>(null) }
        Div({
            classes("t-convo-scroll")
            ref { scroller = it; onDispose { scroller = null } }
        }) {
            Notice(TalkIcons.Info, stringResource(MR.strings.channels_not_kept))
            if (channel.level == ChannelLevel.Block && !channel.isTeleport) Notice(TalkIcons.Pin, stringResource(MR.strings.channels_block_warning))
            model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
            val teleported = stringResource(MR.strings.channels_teleported)
            model.messages.forEach { message ->
                val time = message.createdAt.toLocalDateTime(zone).time
                Div({ classes("t-line"); if (message.isMine) classes("t-line-mine") }) {
                    Span({ classes("t-nick") }) { Text(message.displayName) }
                    if (message.isTeleported) Span({ classes("t-on-blue-plain", "t-inline") }) { TalkIcon(TalkIcons.Teleport, teleported, size = 16f) }
                    Span({ classes("t-caption") }) { Text(" · ${time.hour}:${time.minute.toString().padStart(2, '0')}") }
                    Div({ classes("t-line-text") }) { Text(message.content) }
                }
            }
        }
        LaunchedEffect(model.messages.size, scroller) { scroller?.let { it.scrollTop = it.scrollHeight.toDouble() } }
        val placeholder = stringResource(MR.strings.channels_message_as, channel.geohash, model.nickname)
        Div({ classes("t-chat-bar") }) {
            Label(forId = "room-input", attrs = { classes("t-sr") }) { Text(placeholder) }
            Input(InputType.Text) {
                id("room-input")
                classes("t-chat-input")
                attr("placeholder", placeholder)
                attr("autocomplete", "off")
                value(model.text)
                onInput { component.onTextChanged(it.value) }
                onKeyDown { if (it.key == "Enter" && !it.isComposing) { it.preventDefault(); if (model.canSend) component.onSend() } }
            }
            IconButton(TalkIcons.Send, stringResource(MR.strings.chat_send), component::onSend, attrs = { if (!model.canSend) disabled() })
        }
    }
}
