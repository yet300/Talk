package com.yet.talk.ui.screens.chat

import com.yet.talk.ui.components.glass.GlassSegments
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.domain.model.chat.Disappearing
import com.yet.talk.domain.model.moderation.ReportTarget
import com.yet.talk.feature.chat.component.info.ChatInfoComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ActionRow
import com.yet.talk.ui.components.Caption
import com.yet.talk.ui.components.ConfirmDialog
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.PersonRow
import com.yet.talk.ui.components.ReportSheet
import com.yet.talk.ui.components.SectionTitle
import com.yet.talk.ui.components.SwitchRow
import com.yet.talk.ui.components.TalkBottomSheet
import com.yet.talk.ui.components.glass.BackBar
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.screens.chat.component.ChatAvatar
import com.yet.talk.ui.screens.chat.component.chatTitle
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * Chat info (`ChatInfo`): who is in it, disappearing messages, mute, Verify, the encryption explainer sheet (decision
 * 20), and block and report (a person) or leave (a group), each asking first.
 */
@Composable
fun ChatInfoContent(component: ChatInfoComponent) {
    val model by component.model.subscribeAsState()
    var explainer by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf<InfoConfirm?>(null) }
    var reporting by rememberSaveable { mutableStateOf(false) }
    val title = chatTitle(model.name, model.members)
    GlassPage(bar = { BackBar(stringResource(MR.strings.chat_info), onBack = component::onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            item(key = "head") {
                Column(Modifier.fillMaxWidth().padding(MaterialTheme.spacing.lg), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                    ChatAvatar(model.members, size = MaterialTheme.sizes.avatarLg * 1.5f)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(title, style = MaterialTheme.talkTypography.headingLarge, color = MaterialTheme.talkColors.textPrimary)
                        if (model.isVerified) TalkIcon(TalkIcons.Check, stringResource(MR.strings.chat_verified), size = 22.dp, tint = MaterialTheme.talkColors.accent)
                    }
                }
            }
            item(key = "error") { ErrorRow(model.error, onDismiss = component::onErrorShown) }
            if (model.isGroup) {
                item(key = "people") { SectionTitle(stringResource(MR.strings.chat_people)) }
                items(model.members, key = { it.id.value }) { user -> PersonRow(user, onClick = { component.onOpenMember(user.id) }) }
            }
            item(key = "disappearing") {
                SectionTitle(stringResource(MR.strings.chat_disappearing))
                GlassSegments(
                    items = Disappearing.entries,
                    selected = model.disappearing,
                    label = { stringResource(it.label) },
                    onSelect = component::onDisappearingChanged,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
                    segmentWidth = 80.dp,
                )
                Caption(stringResource(MR.strings.chat_disappearing_note))
            }
            item(key = "mute") {
                SwitchRow(stringResource(MR.strings.chat_mute), stringResource(MR.strings.chat_mute_note), model.isMuted, component::onMutedChanged, icon = TalkIcons.Mute)
            }
            item(key = "actions") {
                if (!model.isGroup) {
                    ActionRow(stringResource(MR.strings.chat_verify), component::onVerify, icon = TalkIcons.Shield, note = stringResource(MR.strings.chat_verified).takeIf { model.isVerified })
                }
                ActionRow(stringResource(MR.strings.chat_encryption_row), { explainer = true }, icon = TalkIcons.Lock)
                if (model.isGroup) {
                    ActionRow(stringResource(MR.strings.chat_leave), { confirm = InfoConfirm.Leave }, icon = TalkIcons.Back, danger = true)
                } else {
                    if (model.isBlocked) ActionRow(stringResource(MR.strings.action_unblock), { component.onBlockedChanged(false) }, icon = TalkIcons.Block)
                    else ActionRow(stringResource(MR.strings.action_block), { confirm = InfoConfirm.Block }, icon = TalkIcons.Block, danger = true)
                    if (model.members.isNotEmpty()) ActionRow(stringResource(MR.strings.action_report), { reporting = true }, icon = TalkIcons.Report, danger = true)
                }
            }
        }
    }
    if (explainer) {
        TalkBottomSheet(onDismiss = { explainer = false }, title = stringResource(MR.strings.chat_encryption)) {
            listOf(MR.strings.chat_encryption_1, MR.strings.chat_encryption_2, MR.strings.chat_encryption_3).forEach {
                Text(stringResource(it), style = MaterialTheme.talkTypography.body, color = MaterialTheme.talkColors.textPrimary, modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl, vertical = MaterialTheme.spacing.sm))
            }
        }
    }
    val person = model.members.firstOrNull()
    if (reporting && person != null) ReportSheet(ReportTarget.Profile(person.id), onDismiss = { reporting = false })
    when (confirm) {
        InfoConfirm.Block -> ConfirmDialog(
            title = stringResource(MR.strings.chat_block_title, title),
            body = stringResource(MR.strings.chat_block_body),
            confirm = stringResource(MR.strings.action_block),
            onConfirm = { confirm = null; component.onBlockedChanged(true) },
            onDismiss = { confirm = null },
        )
        InfoConfirm.Leave -> ConfirmDialog(
            title = stringResource(MR.strings.chat_leave_title),
            body = stringResource(MR.strings.chat_leave_body),
            confirm = stringResource(MR.strings.chat_leave),
            onConfirm = { confirm = null; component.onLeave() },
            onDismiss = { confirm = null },
        )
        null -> Unit
    }
}

/** The confirmation open over chat info (saved across rotation). */
private enum class InfoConfirm { Block, Leave }

private val Disappearing.label
    get() = when (this) {
        Disappearing.Off -> MR.strings.disappearing_off
        Disappearing.Hour -> MR.strings.disappearing_hour
        Disappearing.Day -> MR.strings.disappearing_day
        Disappearing.Week -> MR.strings.disappearing_week
    }
