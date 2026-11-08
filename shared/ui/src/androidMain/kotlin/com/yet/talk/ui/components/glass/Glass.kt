package com.yet.talk.ui.components.glass

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.yet.talk.ui.theme.talkColors
import dev.chrisbanes.haze.glass.OpticalSizeValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle

/**
 * The content behind the glass bars: every screen marks what scrolls under its bars with
 * `Modifier.hazeSource(LocalGlassSource.current)`, and the bars refract it (Haze Glass, design pass 4).
 */
val LocalGlassSource = compositionLocalOf<HazeState> { error("Provided by the screen that owns the glass bars") }

/**
 * Haze's Regular glass in the capsule's and buttons' pill shape, with a little more blur and a veil of the page colour so
 * icons and labels stay legible over busy content (photos, code).
 */
@Composable
internal fun pillGlass(): GlassStyle {
    val veil = MaterialTheme.talkColors.bg.copy(alpha = PILL_VEIL)
    return remember(veil) {
        GlassStyle.regular.then {
            shape(RoundedCornerShape(percent = 50))
            optics { copy(depth = OpticalSizeValue.Fixed(1f), blurRadius = OpticalSizeValue.Fixed(PillBlur)) }
            tint(veil)
        }
    }
}

/**
 * The top bars: Regular glass with square edges, so the bar reads as one sheet across the screen. Text and buttons sit
 * on it, so it diffuses more than Regular (a strong blur) and carries a stronger veil of the page colour.
 */
@Composable
internal fun barGlass(): GlassStyle {
    val veil = MaterialTheme.talkColors.bg.copy(alpha = BAR_VEIL)
    return remember(veil) {
        GlassStyle.regular.then {
            shape(RoundedCornerShape(0.dp))
            // A sheet across the pane, not an object on it: no rim or edge shadow, which drew a box around the bar
            // (and a line against the side column on large screens). A hairline under it marks its edge instead.
            specularIntensity(0f)
            edgeShadow(Color.Transparent)
            // Full depth: Regular keeps the backdrop sharp under short surfaces (a bar is only ~100 dp tall), which left text
            // readable through the bar; a bar should diffuse it completely.
            optics { copy(depth = OpticalSizeValue.Fixed(1f), blurRadius = OpticalSizeValue.Fixed(BarBlur)) }
            tint(veil)
        }
    }
}

/** The page colour under a bar's content, for legibility whether or not the blur renders. */
@Composable
internal fun barVeil(): Color = MaterialTheme.talkColors.bg.copy(alpha = BAR_VEIL)

private val BarBlur = 36.dp
private val PillBlur = 28.dp
private const val BAR_VEIL = 0.86f
private const val PILL_VEIL = 0.55f
