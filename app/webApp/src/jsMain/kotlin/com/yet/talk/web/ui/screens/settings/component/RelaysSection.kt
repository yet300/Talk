package com.yet.talk.web.ui.screens.settings.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.relay.Relay
import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.IconButton
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.components.TextField
import com.yet.talk.web.ui.components.list.SectionTitle
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

/** Relays: one row each (address, read/write, remove), and the `wss://` field with Add. */
@Composable
fun RelaysSection(model: SettingsComponent.Model, component: SettingsComponent) {
    SectionTitle(stringResource(MR.strings.settings_relays))
    model.relays.forEach { relay -> RelayRow(relay, onRemove = { component.onRemoveRelay(relay.url) }) }
    Div({ classes("t-row", "t-add-relay") }) {
        TextField(
            value = model.relayInput,
            onValueChange = component::onRelayInputChanged,
            label = stringResource(MR.strings.settings_relay_address),
            placeholder = stringResource(MR.strings.settings_relay_placeholder),
            id = "relay-url",
            visibleLabel = false,
            onEnter = component::onAddRelay,
        )
        TalkButton(stringResource(MR.strings.settings_add_relay), component::onAddRelay, ButtonStyle.Secondary, enabled = !model.isWorking)
    }
}

@Composable
private fun RelayRow(relay: Relay, onRemove: () -> Unit) {
    Div({ classes("t-row") }) {
        TalkIcon(TalkIcons.Relay, size = 18f)
        Div({ classes("t-row-text") }) {
            Div { Text(relay.url) }
            Div({ classes("t-caption") }) {
                Text(
                    stringResource(
                        when {
                            relay.read && relay.write -> MR.strings.relay_read_write
                            relay.read -> MR.strings.relay_read_only
                            else -> MR.strings.relay_write_only
                        },
                    ),
                )
            }
        }
        IconButton(TalkIcons.Close, stringResource(MR.strings.settings_remove_relay, relay.url), onRemove, size = 18f)
    }
}
