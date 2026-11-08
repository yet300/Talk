package com.yet.talk.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset

/**
 * Motion (the design's motion vocabulary, iOS-like): critically damped springs for anything moved or swapped, a short
 * ease-out fade for content that only changes, a little bounce only where a gesture carried momentum (the like pop,
 * the tab capsule). Compose already scales every animation by the system's animator duration scale, so "Remove
 * animations" makes them all instant; [LocalReducedMotion] additionally turns slides into fades and drops overshoot.
 */
object TalkMotion {
    /** Screens and panes moving in and out. */
    fun <T> move(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    val moveOffset: FiniteAnimationSpec<IntOffset> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Content changing in place (loading → content → empty). */
    fun <T> fade(): FiniteAnimationSpec<T> = tween(durationMillis = 200)

    /** A toggle the finger just flipped: a small overshoot. */
    fun <T> pop(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)

    /** Colours and small state changes. */
    fun <T> settle(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}

/** True when the system's animator scale is 0 ("Remove animations"); provided by [TalkTheme]. */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
internal fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

val reducedMotion: Boolean
    @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
