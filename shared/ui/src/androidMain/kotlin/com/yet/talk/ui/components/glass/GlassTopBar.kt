package com.yet.talk.ui.components.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.talkColors
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.glass.hazeGlass
import dev.icerock.moko.resources.ImageResource

/**
 * A glass top bar (design pass 4): content scrolls under it edge to edge, the status bar included; controls float in
 * round glass buttons. With [glass] false the bar is clear (Profile over its banner) and only the buttons are glass.
 */
@Composable
fun GlassTopBar(
    modifier: Modifier = Modifier,
    glass: Boolean = true,
    leading: @Composable RowScope.() -> Unit = {},
    title: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (glass) Modifier.hazeGlass(input = HazeInput.Backdrop(LocalGlassSource.current), style = barGlass()) else Modifier)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MaterialTheme.sizes.barHeight).padding(horizontal = MaterialTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            title()
            trailing()
        }
    }
}

/** A round glass button in a bar (`t-icon-btn` on glass), [label] is its accessible name. */
@Composable
fun GlassIconButton(icon: ImageResource, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(MaterialTheme.sizes.touchMin)
            .hazeGlass(input = HazeInput.Backdrop(LocalGlassSource.current), style = pillGlass())
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        TalkIcon(icon, contentDescription = null, tint = MaterialTheme.talkColors.textPrimary)
    }
}
