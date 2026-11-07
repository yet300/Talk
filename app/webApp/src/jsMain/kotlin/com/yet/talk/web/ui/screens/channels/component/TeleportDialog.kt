package com.yet.talk.web.ui.screens.channels.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.domain.model.channel.ChannelLevel
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.TextField
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

/** Teleport (`Teleport`): an area code and a level; the map from the design comes later. */
@Composable
fun TeleportDialog(onOpen: (String, ChannelLevel) -> Unit, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var level by remember { mutableStateOf(ChannelLevel.City) }
    val title = stringResource(MR.strings.channels_teleport)
    val levelLabel = stringResource(MR.strings.channels_level)
    Div({ classes("x-scrim", "t-layer"); onClick { onDismiss() } }) {
        Div({
            classes("x-modal", "t-dialog")
            attr("role", "dialog")
            attr("aria-modal", "true")
            attr("aria-label", title)
            onClick { it.stopPropagation() }
        }) {
            H2({ classes("t-h1", "t-flush") }) { Text(title) }
            P({ classes("t-flush") }) { Text(stringResource(MR.strings.channels_teleport_body)) }
            TextField(code, { code = it }, stringResource(MR.strings.channels_area_code), "u4pru", "teleport-code") { onOpen(code, level) }
            Div({ classes("t-chips", "t-chips-flush"); attr("role", "radiogroup"); attr("aria-label", levelLabel) }) {
                ChannelLevel.entries.forEach { option ->
                    val on = option == level
                    Button({
                        classes("t-chip")
                        if (on) classes("t-chip-on")
                        type(ButtonType.Button)
                        attr("role", "radio")
                        attr("aria-checked", on.toString())
                        onClick { level = option }
                    }) { Text(stringResource(option.label)) }
                }
            }
            TalkButton(stringResource(MR.strings.channels_open), { onOpen(code, level) }, ButtonStyle.Post, attrs = {
                if (code.isBlank()) attr("disabled", "")
            })
            TalkButton(stringResource(MR.strings.action_cancel), onDismiss, ButtonStyle.Secondary, attrs = { classes("t-btn-44") })
        }
    }
}
