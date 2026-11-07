package com.yet.talk.web.ui.screens.channels.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.channel.ChannelLevel
import com.yet.talk.feature.channels.ChannelsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.pluralResource
import com.yet.talk.web.ui.theme.stringResource
import dev.icerock.moko.resources.ImageResource
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** The five levels where you are, with people counts ("? people" where nobody announces), Teleport, the nickname. */
@Composable
fun ChannelList(model: ChannelsComponent.Model, component: ChannelsComponent) {
    P({ classes("t-caption", "t-chats-note") }) { Text(stringResource(MR.strings.channels_caption)) }
    when (model.location) {
        ChannelsComponent.Location.Locating -> P({ classes("t-caption", "t-search-hint"); attr("role", "status") }) { Text(stringResource(MR.strings.search_searching)) }
        ChannelsComponent.Location.Unavailable, ChannelsComponent.Location.Unknown -> Unit
        ChannelsComponent.Location.Known -> model.here.forEach { channel ->
            val count = model.people[channel.geohash]
            ChannelRow(
                icon = TalkIcons.Pin,
                title = stringResource(channel.level.label),
                caption = "#${channel.geohash}",
                trailing = if (channel.level.sendsPresence) count?.let { pluralResource(MR.plurals.channels_people, it) }.orEmpty()
                else stringResource(MR.strings.channels_people_unknown),
                onClick = { component.onOpen(channel) },
            )
        }
    }
    ChannelRow(
        icon = TalkIcons.Teleport,
        title = stringResource(MR.strings.channels_teleport),
        caption = stringResource(MR.strings.channels_teleport_caption),
        trailing = null,
        onClick = { component.onShowTeleport(true) },
    )
    Div({ classes("t-row") }) {
        Span({ classes("t-row-text") }) {
            Span({ classes("x-name", "t-block") }) { Text(stringResource(MR.strings.channels_nickname_here, model.nickname)) }
            Span({ classes("t-caption", "t-block") }) { Text(stringResource(MR.strings.channels_nickname_caption)) }
        }
        TalkButton(stringResource(MR.strings.action_change), component::onEditNickname, ButtonStyle.Secondary)
    }
}

@Composable
private fun ChannelRow(icon: ImageResource, title: String, caption: String, trailing: String?, onClick: () -> Unit) {
    Button({
        classes("t-row", "t-row-button")
        type(ButtonType.Button)
        onClick { onClick() }
    }) {
        Span({ classes("x-avatar", "t-channel-icon") }) { TalkIcon(icon, size = 18f) }
        Span({ classes("t-row-text") }) {
            Span({ classes("x-name", "t-block") }) { Text(title) }
            Span({ classes("t-caption", "t-block") }) { Text(caption) }
        }
        if (trailing != null) Span({ classes("t-caption") }) { Text(trailing) }
    }
}

internal val ChannelLevel.label
    get() = when (this) {
        ChannelLevel.Region -> MR.strings.level_region
        ChannelLevel.Province -> MR.strings.level_province
        ChannelLevel.City -> MR.strings.level_city
        ChannelLevel.Neighborhood -> MR.strings.level_neighborhood
        ChannelLevel.Block -> MR.strings.level_block
    }
