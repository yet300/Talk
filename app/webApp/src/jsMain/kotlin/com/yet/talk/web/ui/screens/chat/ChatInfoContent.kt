package com.yet.talk.web.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.domain.model.chat.Disappearing
import com.yet.talk.domain.model.moderation.ReportTarget
import com.yet.talk.feature.chat.component.info.ChatInfoComponent
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.ConfirmDialog
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.components.list.PersonRow
import com.yet.talk.web.ui.components.list.SectionTitle
import com.yet.talk.web.ui.components.moderation.ReportMenu
import com.yet.talk.web.ui.screens.chat.component.ChatAvatar
import com.yet.talk.web.ui.screens.chat.component.chatTitle
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import dev.icerock.moko.resources.ImageResource
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Chat info (`ChatInfo`): who is in it, disappearing messages, mute, block and report (a person) or leave (a group),
 * and the encryption explainer sheet (decision 20).
 */
@Composable
fun ChatInfoContent(component: ChatInfoComponent) {
    val model by component.model.subscribeAsState()
    var explainer by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var reporting by remember { mutableStateOf(false) }
    val title = chatTitle(model.name, model.members)
    TopBar(title = stringResource(MR.strings.chat_info), onBack = component::onBack)
    Div({ classes("t-info-head") }) {
        ChatAvatar(model.members)
        H2({ classes("t-h1", "t-flush") }) { Text(title) }
    }
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }

    if (model.isGroup) {
        SectionTitle(stringResource(MR.strings.chat_people))
        model.members.forEach { user -> PersonRow(user, onClick = { component.onOpenMember(user.id) }) }
    }

    val disappearingLabel = stringResource(MR.strings.chat_disappearing)
    SectionTitle(disappearingLabel)
    Div({ classes("t-seg", "t-info-seg"); attr("role", "radiogroup"); attr("aria-label", disappearingLabel) }) {
        Disappearing.entries.forEach { option ->
            val active = option == model.disappearing
            Button({
                classes("t-seg-item")
                if (active) classes("is-active")
                type(ButtonType.Button)
                attr("role", "radio")
                attr("aria-checked", active.toString())
                onClick { component.onDisappearingChanged(option) }
            }) { Text(stringResource(option.label)) }
        }
    }
    P({ classes("t-caption", "t-info-note") }) { Text(stringResource(MR.strings.chat_disappearing_note)) }

    val muteLabel = stringResource(MR.strings.chat_mute)
    Div({ classes("t-row", "t-info-row") }) {
        TalkIcon(TalkIcons.Mute)
        Span({ classes("t-row-text") }) {
            Span({ classes("x-name", "t-block") }) { Text(stringResource(MR.strings.chat_mute)) }
            Span({ classes("t-caption", "t-block") }) { Text(stringResource(MR.strings.chat_mute_note)) }
        }
        Button({
            classes("t-switch")
            type(ButtonType.Button)
            attr("role", "switch")
            attr("aria-checked", model.isMuted.toString())
            attr("aria-label", muteLabel)
            onClick { component.onMutedChanged(!model.isMuted) }
        })
    }

    InfoAction(TalkIcons.Lock, stringResource(MR.strings.chat_encryption_row)) { explainer = true }
    if (model.isGroup) {
        InfoAction(TalkIcons.Back, stringResource(MR.strings.chat_leave), danger = true) { confirm = Confirm.Leave }
    } else {
        val person = model.members.firstOrNull()
        if (model.isBlocked) {
            InfoAction(TalkIcons.Block, stringResource(MR.strings.action_unblock)) { component.onBlockedChanged(false) }
        } else {
            InfoAction(TalkIcons.Block, stringResource(MR.strings.action_block), danger = true) { confirm = Confirm.Block }
        }
        if (person != null) {
            Div({ classes("t-menu-host") }) {
                InfoAction(TalkIcons.Report, stringResource(MR.strings.action_report), danger = true) { reporting = !reporting }
                if (reporting) ReportMenu(ReportTarget.Profile(person.id), onDismiss = { reporting = false })
            }
        }
    }

    if (explainer) EncryptionSheet(onDismiss = { explainer = false })
    when (confirm) {
        Confirm.Block -> ConfirmDialog(
            title = stringResource(MR.strings.chat_block_title, title),
            body = stringResource(MR.strings.chat_block_body),
            confirm = stringResource(MR.strings.action_block),
            onConfirm = { confirm = null; component.onBlockedChanged(true) },
            onDismiss = { confirm = null },
        )
        Confirm.Leave -> ConfirmDialog(
            title = stringResource(MR.strings.chat_leave_title),
            body = stringResource(MR.strings.chat_leave_body),
            confirm = stringResource(MR.strings.chat_leave),
            onConfirm = { confirm = null; component.onLeave() },
            onDismiss = { confirm = null },
        )
        null -> Unit
    }
}

private enum class Confirm { Block, Leave }

@Composable
private fun InfoAction(icon: ImageResource, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Button({
        classes("t-row", "t-row-button")
        if (danger) classes("t-danger")
        type(ButtonType.Button)
        onClick { onClick() }
    }) {
        TalkIcon(icon)
        Span({ classes("t-row-text", "x-name") }) { Text(label) }
    }
}

/** The encryption explainer (`ChatInfo-Encryption`): three plain lines, no PIN anywhere. */
@Composable
private fun EncryptionSheet(onDismiss: () -> Unit) {
    val title = stringResource(MR.strings.chat_encryption)
    Div({ classes("t-layer", "x-scrim"); onClick { onDismiss() } }) {
        Div({
            classes("x-modal", "t-dialog")
            attr("role", "dialog")
            attr("aria-modal", "true")
            attr("aria-label", title)
            onClick { it.stopPropagation() }
        }) {
            H2({ classes("t-h1", "t-flush") }) { Text(title) }
            listOf(MR.strings.chat_encryption_1, MR.strings.chat_encryption_2, MR.strings.chat_encryption_3).forEach {
                P({ classes("t-flush") }) { Text(stringResource(it)) }
            }
            TalkButton(stringResource(MR.strings.action_close), onDismiss, ButtonStyle.Secondary, attrs = { classes("t-btn-44") })
        }
    }
}

private val Disappearing.label
    get() = when (this) {
        Disappearing.Off -> MR.strings.disappearing_off
        Disappearing.Hour -> MR.strings.disappearing_hour
        Disappearing.Day -> MR.strings.disappearing_day
        Disappearing.Week -> MR.strings.disappearing_week
    }
