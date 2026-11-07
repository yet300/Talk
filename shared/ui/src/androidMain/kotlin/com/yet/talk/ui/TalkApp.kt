package com.yet.talk.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.feature.root.RootComponent
import com.yet.talk.ui.root.RootContent
import com.yet.talk.ui.theme.TalkTheme

/** The whole app: the theme chosen in Settings (Light, Dark or System) around the root stack. */
@Composable
fun TalkApp(root: RootComponent) {
    val theme by root.theme.subscribeAsState()
    val system = isSystemInDarkTheme()
    TalkTheme(
        darkTheme = when (theme) {
            ThemeMode.Dark -> true
            ThemeMode.Light -> false
            ThemeMode.System -> system
        },
    ) {
        RootContent(root)
    }
}
