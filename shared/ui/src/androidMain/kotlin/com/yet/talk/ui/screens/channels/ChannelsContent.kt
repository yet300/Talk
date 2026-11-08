package com.yet.talk.ui.screens.channels

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.channels.ChannelsComponent
import com.yet.talk.feature.channels.ChannelsComponent.Step
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.TalkTextField
import com.yet.talk.ui.components.glass.BackBar
import com.yet.talk.ui.components.glass.GlassIconButton
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.navigation.AnimatedSlot
import com.yet.talk.ui.screens.channels.component.ChannelList
import com.yet.talk.ui.screens.channels.dialog.TeleportDialog
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.TalkMotion
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource

/**
 * Location channels (`LocationChannels`, `Channels-Intro`): the first run (what they are, the location step with
 * Android's coarse location permission, a nickname) stepping forward, then the five levels and Teleport; the open
 * room (`ChildSlot`) slides over it and goes back with the predictive back gesture.
 */
@Composable
fun ChannelsContent(component: ChannelsComponent) {
    val model by component.model.subscribeAsState()
    val room by component.room.subscribeAsState()
    // The permission answer, either way, goes on to the component: a denial ends up as "teleport only".
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { component.onAllowLocation() }
    Box(Modifier.fillMaxSize()) {
        GlassPage(
            modifier = Modifier.imePadding(),
            bar = {
                BackBar(stringResource(MR.strings.channels_title), onBack = component::onBack) {
                    if (model.step == Step.List) GlassIconButton(TalkIcons.Teleport, stringResource(MR.strings.channels_teleport), { component.onShowTeleport(true) })
                }
            },
        ) { padding ->
            AnimatedContent(
                model.step,
                transitionSpec = { (slideInHorizontally(TalkMotion.moveOffset) { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally(TalkMotion.moveOffset) { -it / 3 } + fadeOut()) },
                label = "step",
            ) { step ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    ErrorRow(model.error, onDismiss = component::onErrorShown)
                    when (step) {
                        Step.Loading -> Unit
                        Step.Intro -> IntroStep(TalkIcons.Pin, MR.strings.channels_title) {
                            Body(stringResource(MR.strings.channels_intro_body))
                            Body(stringResource(MR.strings.channels_not_kept), muted = true)
                            TalkButton(stringResource(MR.strings.action_continue), component::onIntroContinue, ButtonStyle.Post, fullWidth = true)
                            TalkButton(stringResource(MR.strings.action_not_now), component::onBack, ButtonStyle.Secondary, fullWidth = true)
                        }
                        Step.LocationStep -> IntroStep(TalkIcons.Shield, MR.strings.channels_location_title) {
                            Body(stringResource(MR.strings.channels_location_body))
                            TalkButton(stringResource(MR.strings.channels_allow_location), { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }, ButtonStyle.Post, fullWidth = true)
                            TalkButton(stringResource(MR.strings.channels_teleport_instead), component::onTeleportInstead, ButtonStyle.Secondary, fullWidth = true)
                        }
                        Step.Nickname -> IntroStep(TalkIcons.User, MR.strings.channels_nickname_title) {
                            TalkTextField(model.nicknameDraft, component::onNicknameChanged, label = stringResource(MR.strings.channels_nickname), placeholder = "anon", onDone = component::onSaveNickname)
                            Body(stringResource(MR.strings.channels_nickname_body), muted = true)
                            TalkButton(stringResource(MR.strings.channels_join), component::onSaveNickname, ButtonStyle.Post, fullWidth = true)
                        }
                        Step.List -> ChannelList(model, component)
                    }
                }
            }
        }
        AnimatedSlot(room, component.backHandler, onBack = { it.onBack() }) { RoomContent(it) }
    }
    if (model.isTeleportOpen) TeleportDialog(onOpen = component::onTeleport, onDismiss = { component.onShowTeleport(false) })
}

@Composable
private fun IntroStep(icon: ImageResource, title: StringResource, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 420.dp).padding(MaterialTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
        ) {
            TalkIcon(icon, null, size = MaterialTheme.sizes.iconEmpty, tint = MaterialTheme.talkColors.textSecondary)
            Text(stringResource(title), style = MaterialTheme.talkTypography.headingLarge, color = MaterialTheme.talkColors.textPrimary)
            content()
        }
    }
}

@Composable
private fun Body(text: String, muted: Boolean = false) {
    Text(text, style = MaterialTheme.talkTypography.body, color = if (muted) MaterialTheme.talkColors.textSecondary else MaterialTheme.talkColors.textPrimary)
}
