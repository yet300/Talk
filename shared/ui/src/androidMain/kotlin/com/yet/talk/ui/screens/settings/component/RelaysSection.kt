package com.yet.talk.ui.screens.settings.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yet.talk.domain.model.network.RelayState
import com.yet.talk.domain.model.relay.Relay
import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.ListRow
import com.yet.talk.ui.components.SectionTitle
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.TalkIconButton
import com.yet.talk.ui.components.TalkTextField
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.TalkMotion
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors

/** Relays: a row each (address, state dot, `Connected · Read and write`, remove), and the `wss://` field with Add. */
@Composable
fun RelaysSection(model: SettingsComponent.Model, component: SettingsComponent) {
    SectionTitle(stringResource(MR.strings.settings_relays))
    model.relays.forEach { relay ->
        androidx.compose.runtime.key(relay.url) { RelayRow(relay, model.relayStates[relay.url] ?: RelayState.NotUsedYet, onRemove = { component.onRemoveRelay(relay.url) }) }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        TalkTextField(
            model.relayInput, component::onRelayInputChanged,
            placeholder = stringResource(MR.strings.settings_relay_placeholder),
            modifier = Modifier.weight(1f),
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
            onDone = component::onAddRelay,
        )
        TalkButton(stringResource(MR.strings.settings_add_relay), component::onAddRelay, ButtonStyle.Secondary, enabled = !model.isWorking)
    }
}

@Composable
private fun RelayRow(relay: Relay, state: RelayState, onRemove: () -> Unit) {
    val colors = MaterialTheme.talkColors
    val dot by animateColorAsState(
        when (state) {
            RelayState.Connected -> colors.repost
            RelayState.Connecting -> colors.warning
            RelayState.Unreachable -> colors.error
            RelayState.NotUsedYet -> colors.textSecondary
        },
        TalkMotion.settle(), label = "state",
    )
    val access = stringResource(
        when {
            relay.read && relay.write -> MR.strings.relay_read_write
            relay.read -> MR.strings.relay_read_only
            else -> MR.strings.relay_write_only
        },
    )
    ListRow(
        title = relay.url,
        caption = "${stringResource(state.label)} · $access",
        leading = {
            Box {
                TalkIcon(TalkIcons.Relay, null, size = 20.dp, tint = colors.textSecondary)
                Box(Modifier.align(Alignment.BottomEnd).size(8.dp).clip(CircleShape).drawBehind { drawRect(dot) })
            }
        },
        trailing = { TalkIconButton(TalkIcons.Close, stringResource(MR.strings.settings_remove_relay, relay.url), onRemove, size = 18.dp) },
    )
}

private val RelayState.label
    get() = when (this) {
        RelayState.Connected -> MR.strings.relay_connected
        RelayState.Connecting -> MR.strings.relay_connecting
        RelayState.Unreachable -> MR.strings.relay_unreachable
        RelayState.NotUsedYet -> MR.strings.relay_not_used
    }
