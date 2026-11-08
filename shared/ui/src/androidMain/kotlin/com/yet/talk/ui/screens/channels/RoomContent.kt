package com.yet.talk.ui.screens.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.domain.model.channel.ChannelLevel
import com.yet.talk.feature.channels.component.room.RoomComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.Notice
import com.yet.talk.ui.components.TalkIconButton
import com.yet.talk.ui.components.TalkTextField
import com.yet.talk.ui.components.glass.GlassIconButton
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.components.glass.GlassTopBar
import com.yet.talk.ui.screens.channels.component.label
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.pluralResource
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A channel room (`Channel-Room-Mobile`): `#area · Level` with the people count, "public and not kept", a plain
 * timeline of `nick#abcd` lines with the teleport mark, and the composer. At Block level, decision 21's warning.
 */
@Composable
fun RoomContent(component: RoomComponent) {
    val model by component.model.subscribeAsState()
    val channel = model.channel
    val colors = MaterialTheme.talkColors
    val zone = remember { TimeZone.currentSystemDefault() }
    val list = rememberLazyListState()
    LaunchedEffect(model.messages.size) { if (model.messages.isNotEmpty()) list.animateScrollToItem(list.layoutInfo.totalItemsCount - 1) }
    GlassPage(
        modifier = Modifier.imePadding(),
        bar = {
            GlassTopBar(
                leading = { GlassIconButton(TalkIcons.Back, stringResource(MR.strings.action_back), component::onBack) },
                title = {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(MR.strings.channels_room_title, channel.geohash, stringResource(channel.level.label)), style = MaterialTheme.talkTypography.bodyBold, color = colors.textPrimary, maxLines = 1)
                        val people = model.people?.let { pluralResource(MR.plurals.channels_people, it) } ?: stringResource(MR.strings.channels_people_unknown)
                        Text(if (channel.isTeleport) "$people · ${stringResource(MR.strings.channels_teleported_here)}" else people, style = MaterialTheme.talkTypography.small, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = MaterialTheme.spacing.sm)) {
                item(key = "kept") { Notice(TalkIcons.Info, stringResource(MR.strings.channels_not_kept)) }
                if (channel.level == ChannelLevel.Block && !channel.isTeleport) item(key = "block") { Notice(TalkIcons.Pin, stringResource(MR.strings.channels_block_warning)) }
                item(key = "error") { ErrorRow(model.error, onDismiss = component::onErrorShown) }
                items(model.messages, key = { it.id }) { message ->
                    val time = message.createdAt.toLocalDateTime(zone).time
                    Column(Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.lg, vertical = 4.dp).animateItem()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(message.displayName, style = MaterialTheme.talkTypography.bodyBold, color = if (message.isMine) colors.accent else colors.textPrimary)
                            if (message.isTeleported) TalkIcon(TalkIcons.Teleport, stringResource(MR.strings.channels_teleported), size = 14.dp, tint = colors.accent)
                            Text("· ${time.hour}:${time.minute.toString().padStart(2, '0')}", style = MaterialTheme.talkTypography.small, color = colors.textSecondary)
                        }
                        Text(message.content, style = MaterialTheme.talkTypography.body, color = colors.textPrimary)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().background(colors.bg).navigationBarsPadding().padding(MaterialTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                TalkTextField(
                    model.text, component::onTextChanged,
                    placeholder = stringResource(MR.strings.channels_message_as, channel.geohash, model.nickname),
                    modifier = Modifier.weight(1f), pill = true, imeAction = ImeAction.Send,
                    onDone = { if (model.canSend) component.onSend() },
                )
                TalkIconButton(TalkIcons.Send, stringResource(MR.strings.chat_send), component::onSend, enabled = model.canSend, tint = colors.accent)
            }
        }
    }
}
