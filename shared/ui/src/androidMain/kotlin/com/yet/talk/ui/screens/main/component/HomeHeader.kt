package com.yet.talk.ui.screens.main.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.Avatar
import com.yet.talk.ui.components.glass.GlassIconButton
import com.yet.talk.ui.components.glass.GlassTopBar
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/** The home header on phones (`x-mobile-header`): your avatar (Profile), the wordmark, Settings, on glass. */
@Composable
fun HomeHeader(me: TabsComponent.Me, onProfile: () -> Unit, onSettings: () -> Unit) {
    GlassTopBar(
        leading = { Avatar(me.user, onClick = onProfile) },
        title = {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(stringResource(MR.strings.app_name), style = MaterialTheme.talkTypography.wordmark, color = MaterialTheme.talkColors.textPrimary)
            }
        },
        trailing = { GlassIconButton(TalkIcons.Settings, stringResource(MR.strings.action_settings), onSettings) },
    )
}
