package com.yet.talk.ui.screens.settings.component

import com.yet.talk.ui.components.glass.GlassSegments
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.resources.*
import com.yet.talk.ui.components.SectionTitle
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors

/** Theme: Dark · Light · System (the design's order, decision 7). */
@Composable
fun ThemeSection(theme: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SectionTitle(stringResource(MR.strings.settings_theme))
    val modes = listOf(ThemeMode.Dark, ThemeMode.Light, ThemeMode.System)
    // The app's own segmented capsule (as Home's feeds and the profile tabs), not Material's outlined buttons.
    GlassSegments(
        items = modes,
        selected = theme,
        label = { mode ->
            stringResource(
                when (mode) {
                    ThemeMode.Dark -> MR.strings.theme_dark
                    ThemeMode.Light -> MR.strings.theme_light
                    ThemeMode.System -> MR.strings.theme_system
                },
            )
        },
        onSelect = onSelect,
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
    )
}
