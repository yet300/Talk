package com.yet.talk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val LocalTalkColors = staticCompositionLocalOf<TalkColors> { error("TalkColors are provided by TalkTheme") }
private val LocalTalkTypography = staticCompositionLocalOf { DefaultTalkTypography }
private val LocalTalkSpacing = staticCompositionLocalOf { TalkSpacing() }
private val LocalTalkSizes = staticCompositionLocalOf { TalkSizes() }
private val LocalTalkShapes = staticCompositionLocalOf { TalkShapes() }

/**
 * The X (Twitter) design system on Material 3. Material components get a matching [ColorScheme],
 * typography and shapes; everything else is read through the `MaterialTheme.talk…` extensions.
 */
@Composable
fun TalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = remember(context, darkTheme) { talkColors(context, darkTheme) }
    val typography = DefaultTalkTypography
    val shapes = remember { TalkShapes() }
    CompositionLocalProvider(
        LocalTalkColors provides colors,
        LocalTalkTypography provides typography,
        LocalTalkShapes provides shapes,
        LocalReducedMotion provides rememberSystemReducedMotion(),
    ) {
        MaterialTheme(
            colorScheme = remember(colors) { colors.toColorScheme() },
            typography = remember(typography) { typography.toMaterial() },
            shapes = remember(shapes) { shapes.toMaterial() },
            content = content,
        )
    }
}

/** Every design-system colour; [MaterialTheme.colorScheme] holds only the ones Material needs. */
val MaterialTheme.talkColors: TalkColors
    @Composable @ReadOnlyComposable get() = LocalTalkColors.current

/** The design's type scale; [MaterialTheme.typography] maps it onto Material's slots. */
val MaterialTheme.talkTypography: TalkTypography
    @Composable @ReadOnlyComposable get() = LocalTalkTypography.current

val MaterialTheme.spacing: TalkSpacing
    @Composable @ReadOnlyComposable get() = LocalTalkSpacing.current

val MaterialTheme.sizes: TalkSizes
    @Composable @ReadOnlyComposable get() = LocalTalkSizes.current

/** Corner radii by role; [MaterialTheme.shapes] maps them onto Material's sizes. */
val MaterialTheme.talkShapes: TalkShapes
    @Composable @ReadOnlyComposable get() = LocalTalkShapes.current

private fun TalkColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = tintBlue,
        onPrimaryContainer = accent,
        secondary = textSecondary,
        onSecondary = bg,
        tertiary = repost,
        background = bg,
        onBackground = textPrimary,
        surface = bg,
        onSurface = textPrimary,
        surfaceVariant = surface,
        onSurfaceVariant = textSecondary,
        surfaceTint = bg,
        surfaceDim = bg,
        surfaceBright = elevated,
        surfaceContainerLowest = bg,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = elevated,
        surfaceContainerHighest = elevated,
        inverseSurface = textPrimary,
        inverseOnSurface = bg,
        inversePrimary = accent,
        outline = controlBorder,
        outlineVariant = border,
        error = error,
        onError = onAccent,
        scrim = modalScrim,
    )
}
