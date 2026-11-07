package com.yet.talk.web.ui.screens.channels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.yet.talk.feature.channels.ChannelsComponent
import com.yet.talk.feature.channels.ChannelsComponent.Step
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.IconButton
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.TextField
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.screens.channels.component.ChannelList
import com.yet.talk.web.ui.screens.channels.component.TeleportDialog
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import dev.icerock.moko.resources.ImageResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Location channels (`LocationChannels`, `ChannelsIntro`): the first run (what they are, the location step, a
 * nickname), then the list; a room covers it while open.
 */
@Composable
fun ChannelsContent(component: ChannelsComponent) {
    val model by component.model.subscribeAsState()
    val room by component.room.subscribeAsState()
    room.child?.instance?.let { return RoomContent(it) }
    TopBar(title = stringResource(MR.strings.channels_title), onBack = component::onBack) {
        if (model.step == Step.List) IconButton(TalkIcons.Teleport, stringResource(MR.strings.channels_teleport), { component.onShowTeleport(true) })
    }
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
    when (model.step) {
        Step.Loading -> Unit
        Step.Intro -> IntroStep(TalkIcons.Pin, MR.strings.channels_title) {
            P({ classes("t-flush") }) { Text(stringResource(MR.strings.channels_intro_body)) }
            P({ classes("t-muted", "t-flush") }) { Text(stringResource(MR.strings.channels_not_kept)) }
            TalkButton(stringResource(MR.strings.action_continue), component::onIntroContinue, ButtonStyle.Post)
            TalkButton(stringResource(MR.strings.action_not_now), component::onBack, ButtonStyle.Secondary, attrs = { classes("t-btn-44") })
        }
        Step.LocationStep -> IntroStep(TalkIcons.Shield, MR.strings.channels_location_title) {
            P({ classes("t-flush") }) { Text(stringResource(MR.strings.channels_location_body)) }
            TalkButton(stringResource(MR.strings.channels_allow_location), component::onAllowLocation, ButtonStyle.Post)
            TalkButton(stringResource(MR.strings.channels_teleport_instead), component::onTeleportInstead, ButtonStyle.Secondary, attrs = { classes("t-btn-44") })
        }
        Step.Nickname -> IntroStep(TalkIcons.User, MR.strings.channels_nickname_title) {
            TextField(
                value = model.nicknameDraft,
                onValueChange = component::onNicknameChanged,
                label = stringResource(MR.strings.channels_nickname),
                placeholder = "anon",
                id = "channel-nickname",
                onEnter = component::onSaveNickname,
            )
            P({ classes("t-muted", "t-flush") }) { Text(stringResource(MR.strings.channels_nickname_body)) }
            TalkButton(stringResource(MR.strings.channels_join), component::onSaveNickname, ButtonStyle.Post)
        }
        Step.List -> ChannelList(model, component)
    }
    if (model.isTeleportOpen) TeleportDialog(onOpen = component::onTeleport, onDismiss = { component.onShowTeleport(false) })
}

@Composable
private fun IntroStep(icon: ImageResource, title: dev.icerock.moko.resources.StringResource, content: @Composable () -> Unit) {
    Div({ classes("t-channels-intro") }) {
        Section({ classes("t-auth", "t-channels-step") }) {
            Span({ classes("t-muted") }) { TalkIcon(icon, size = 48f) }
            H1({ classes("t-h1", "t-flush") }) { Text(stringResource(title)) }
            content()
        }
    }
}
