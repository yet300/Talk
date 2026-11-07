package com.yet.talk.ui.components.glass

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle

/**
 * The content behind the glass bars: every screen marks what scrolls under its bars with
 * `Modifier.hazeSource(LocalGlassSource.current)`, and the bars refract it (Haze Glass, design pass 4).
 */
val LocalGlassSource = compositionLocalOf<HazeState> { error("Provided by the screen that owns the glass bars") }

/** Haze's Regular glass (diffused, legible over text) in the capsule's and buttons' pill shape. */
@Composable
@ReadOnlyComposable
internal fun pillGlass(): GlassStyle = PillGlass

/** The top bars: Regular glass with square edges, so the bar reads as one sheet across the screen. */
@Composable
@ReadOnlyComposable
internal fun barGlass(): GlassStyle = BarGlass

private val PillGlass: GlassStyle = GlassStyle.regular.then { shape(RoundedCornerShape(percent = 50)) }
private val BarGlass: GlassStyle = GlassStyle.regular.then {
    shape(RoundedCornerShape(0.dp))
    specularIntensity(0.2f)
}

