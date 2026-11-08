package com.yet.talk.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.settings.SettingsComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.glass.BackBar
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.screens.settings.component.IdentitySection
import com.yet.talk.ui.screens.settings.component.RelaysSection
import com.yet.talk.ui.screens.settings.component.SecuritySection
import com.yet.talk.ui.screens.settings.component.ThemeSection
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource

/** Settings (`Settings-Mobile`, decision 7): Identity (with the private key), Relays, Theme, Security. */
@Composable
fun SettingsContent(component: SettingsComponent) {
    val model by component.model.subscribeAsState()
    GlassPage(modifier = Modifier.imePadding(), bar = { BackBar(stringResource(MR.strings.settings_title), onBack = component::onBack) }) { padding ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(bottom = MaterialTheme.spacing.xl)) {
            ErrorRow(model.error, onDismiss = component::onErrorShown)
            IdentitySection(model, component)
            RelaysSection(model, component)
            ThemeSection(model.theme, component::onThemeSelected)
            SecuritySection(model, component)
        }
    }
}
