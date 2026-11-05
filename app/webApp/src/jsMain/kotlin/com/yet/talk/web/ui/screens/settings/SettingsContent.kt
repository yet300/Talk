package com.yet.talk.web.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.screens.settings.component.IdentitySection
import com.yet.talk.web.ui.screens.settings.component.RelaysSection
import com.yet.talk.web.ui.screens.settings.component.ThemeSection
import com.yet.talk.web.ui.theme.stringResource

/** Settings (`Settings · Web`): Identity (with the private key), Relays, Theme. */
@Composable
fun SettingsContent(component: SettingsComponent) {
    val model by component.model.subscribeAsState()
    TopBar(stringResource(MR.strings.settings_title))
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
    IdentitySection(model, component)
    RelaysSection(model, component)
    ThemeSection(model.theme, component::onThemeSelected)
}
