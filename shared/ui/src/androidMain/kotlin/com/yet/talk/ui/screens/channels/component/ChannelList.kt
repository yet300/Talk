package com.yet.talk.ui.screens.channels.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yet.talk.domain.model.channel.ChannelLevel
import com.yet.talk.feature.channels.ChannelsComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.Caption
import com.yet.talk.ui.components.ListRow
import com.yet.talk.ui.components.RowIcon
import com.yet.talk.ui.components.SkeletonCards
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.pluralResource
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/** The five levels where you are, with people counts ("? people" where nobody announces), Teleport, the nickname. */
@Composable
fun ChannelList(model: ChannelsComponent.Model, component: ChannelsComponent) {
    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "caption") { Caption(stringResource(MR.strings.channels_caption)) }
        when (model.location) {
            ChannelsComponent.Location.Locating -> item(key = "locating") { SkeletonCards(2) }
            ChannelsComponent.Location.Unavailable -> item(key = "denied") { Caption(stringResource(MR.strings.channels_location_denied)) }
            ChannelsComponent.Location.Unknown -> Unit
            ChannelsComponent.Location.Known -> items(model.here, key = { it.geohash }) { channel ->
                val count = model.people[channel.geohash]
                ListRow(
                    title = stringResource(channel.level.label),
                    caption = "#${channel.geohash}",
                    onClick = { component.onOpen(channel) },
                    leading = { RowIcon(TalkIcons.Pin) },
                    modifier = Modifier.animateItem(),
                    trailing = {
                        Text(
                            if (channel.level.sendsPresence) count?.let { pluralResource(MR.plurals.channels_people, it) }.orEmpty() else stringResource(MR.strings.channels_people_unknown),
                            style = MaterialTheme.talkTypography.small, color = MaterialTheme.talkColors.textSecondary,
                        )
                    },
                )
            }
        }
        item(key = "teleport") {
            ListRow(
                title = stringResource(MR.strings.channels_teleport),
                caption = stringResource(MR.strings.channels_teleport_caption),
                onClick = { component.onShowTeleport(true) },
                leading = { RowIcon(TalkIcons.Teleport) },
            )
        }
        item(key = "nickname") {
            ListRow(
                title = stringResource(MR.strings.channels_nickname_here, model.nickname),
                caption = stringResource(MR.strings.channels_nickname_caption),
                trailing = { TalkButton(stringResource(MR.strings.action_change), component::onEditNickname, ButtonStyle.Secondary) },
            )
        }
    }
}

val ChannelLevel.label
    get() = when (this) {
        ChannelLevel.Region -> MR.strings.level_region
        ChannelLevel.Province -> MR.strings.level_province
        ChannelLevel.City -> MR.strings.level_city
        ChannelLevel.Neighborhood -> MR.strings.level_neighborhood
        ChannelLevel.Block -> MR.strings.level_block
    }
