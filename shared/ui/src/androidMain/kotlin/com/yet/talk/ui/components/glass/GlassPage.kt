package com.yet.talk.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.yet.talk.resources.*
import com.yet.talk.ui.components.networkBarHeight
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** The height of a [GlassTopBar] with the status bar above it and the network bar under it, for list padding. */
val topBarInset: Dp
    @Composable get() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + MaterialTheme.sizes.barHeight + networkBarHeight

val navigationBarInset: Dp
    @Composable get() = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * A page under a glass top bar (design pass 4): [content] scrolls edge to edge under the bar, which refracts it.
 * [content] gets the padding that keeps its first and last items clear of the bar and the navigation bar. [overlay]
 * floats above the content (a composer, a FAB).
 */
@Composable
fun GlassPage(
    bar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val glass = rememberHazeState()
    val padding = PaddingValues(top = topBarInset, bottom = navigationBarInset)
    CompositionLocalProvider(LocalGlassSource provides glass) {
        Box(modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.talkColors.bg).hazeSource(glass)) { content(padding) }
            bar()
            overlay()
        }
    }
}

/** The usual bar: Back (when [onBack] is set), the [title], and [actions]. */
@Composable
fun BackBar(title: String, onBack: (() -> Unit)?, glass: Boolean = true, actions: @Composable RowScope.() -> Unit = {}) {
    GlassTopBar(
        glass = glass,
        leading = { if (onBack != null) GlassIconButton(TalkIcons.Back, stringResource(MR.strings.action_back), onBack) },
        title = {
            Text(
                title,
                style = MaterialTheme.talkTypography.headingMedium,
                color = MaterialTheme.talkColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        },
        trailing = actions,
    )
}
