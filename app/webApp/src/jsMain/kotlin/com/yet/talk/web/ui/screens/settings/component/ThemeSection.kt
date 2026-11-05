package com.yet.talk.web.ui.screens.settings.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.Tabs
import com.yet.talk.web.ui.components.list.SectionTitle
import com.yet.talk.web.ui.theme.stringResource

/** Theme: Dark · Light · System, as tabs (the design's order). */
@Composable
fun ThemeSection(theme: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SectionTitle(stringResource(MR.strings.settings_theme))
    Tabs(
        items = listOf(ThemeMode.Dark, ThemeMode.Light, ThemeMode.System),
        selected = theme,
        label = {
            stringResource(
                when (it) {
                    ThemeMode.Dark -> MR.strings.theme_dark
                    ThemeMode.Light -> MR.strings.theme_light
                    ThemeMode.System -> MR.strings.theme_system
                },
            )
        },
        onSelect = onSelect,
        ariaLabel = stringResource(MR.strings.settings_theme),
    )
}
