package com.yet.talk.ui.screens.channels.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.yet.talk.domain.model.channel.ChannelLevel
import com.yet.talk.domain.model.channel.Geohash
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ConfirmDialog
import com.yet.talk.ui.components.TalkTextField
import com.yet.talk.ui.screens.channels.component.label
import com.yet.talk.ui.screens.tab.videos.component.Chip
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource

/** Teleport (`Teleport`): an area code and a level (the web's map is not on Android); back or Cancel closes it. */
@Composable
fun TeleportDialog(onOpen: (String, ChannelLevel) -> Unit, onDismiss: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf(ChannelLevel.City) }
    ConfirmDialog(
        title = stringResource(MR.strings.channels_teleport),
        body = stringResource(MR.strings.channels_teleport_body),
        confirm = stringResource(MR.strings.channels_open),
        destructive = false,
        confirmEnabled = code.isNotBlank(),
        onConfirm = { onOpen(code, level) },
        onDismiss = onDismiss,
    ) {
        TalkTextField(code, { code = it }, label = stringResource(MR.strings.channels_area_code), placeholder = "u4pru", onDone = { if (code.isNotBlank()) onOpen(code, level) })
        Text(stringResource(MR.strings.channels_level), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            ChannelLevel.entries.forEach { option ->
                Chip(stringResource(option.label), on = option == level) {
                    level = option
                    // An area typed finer than the level is cut to it.
                    val area = Geohash.normalize(code)
                    if (Geohash.isValid(area) && area.length > option.precision) code = area.take(option.precision)
                }
            }
        }
    }
}
