@file:OptIn(ExperimentalDecomposeApi::class)

package com.yet.talk.ui.navigation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.PredictiveBackParams
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimator
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.materialPredictiveBackAnimatable
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.essenty.backhandler.BackHandler
import com.yet.talk.ui.theme.reducedMotion
import kotlin.math.roundToInt

/**
 * A page pushed over another (iOS-like): it slides in from the trailing edge on a critically damped spring while the
 * page below drifts a quarter of the way and dims; back reverses the path. With reduced motion, a short fade.
 */
fun pushAnimator(reduced: Boolean): StackAnimator =
    if (reduced) fade(tween(150)) else stackAnimator(PushSpring) { factor, _ ->
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                // factor 1 → 0: entering from the edge; 0 → -1: leaving under the new page.
                val shift = if (factor > 0) factor else factor * UNDERLAY_SHIFT
                layout(placeable.width, placeable.height) { placeable.placeRelative((placeable.width * shift).roundToInt(), 0) }
            }
            .graphicsLayer { alpha = if (factor < 0) 1f + factor * UNDERLAY_DIM else 1f }
    }

/** Content swapped in place (signed in, a tab): a short crossfade. */
fun swapAnimator(): StackAnimator = fade(tween(180))

/**
 * The stack animation for a stack of pages: [pushAnimator] (or [swap] when the selector says so), and the Material
 * predictive back gesture (Android 14+: the page shrinks and follows the finger) whenever there is something to go back
 * to, driven by [backHandler] and finished by [onBack].
 */
@Composable
fun <C : Any, T : Any> pagesAnimation(
    backHandler: BackHandler,
    onBack: () -> Unit,
    canGoBack: (ChildStack<C, T>) -> Boolean = { it.backStack.isNotEmpty() },
    swap: (Child.Created<C, T>, Child.Created<C, T>) -> Boolean = { _, _ -> false },
): StackAnimation<C, T> {
    val reduced = reducedMotion
    return remember(backHandler, reduced) {
        val push = pushAnimator(reduced)
        val fade = swapAnimator()
        stackAnimation(
            predictiveBackParams = { stack ->
                if (canGoBack(stack)) PredictiveBackParams(backHandler, onBack, animatable = ::materialPredictiveBackAnimatable) else null
            },
            selector = { child, other, _, _ -> if (swap(child, other)) fade else push },
        )
    }
}

private val PushSpring: FiniteAnimationSpec<Float> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f)
private const val UNDERLAY_SHIFT = 0.25f
private const val UNDERLAY_DIM = 0.35f
