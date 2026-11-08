package com.yet.talk.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackEvent
import com.arkivanov.essenty.backhandler.BackHandler
import com.yet.talk.ui.theme.TalkMotion
import com.yet.talk.ui.theme.reducedMotion

/**
 * A [ChildSlot] shown over (or beside) what it belongs to: its child slides in from the trailing edge and out the
 * same way (spatial consistency), a child replacing another crossfades. While [backEnabled], the predictive back
 * gesture is registered in [backHandler] (the slot owner's), so the child shrinks and follows the finger before
 * [onBack] dismisses it; a cancelled gesture springs back.
 */
@Composable
fun <T : Any> AnimatedSlot(
    slot: ChildSlot<*, T>,
    backHandler: BackHandler,
    onBack: (T) -> Unit,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true,
    slide: Boolean = true,
    empty: @Composable () -> Unit = {},
    content: @Composable (T) -> Unit,
) {
    val child = slot.child
    val progress = remember { mutableFloatStateOf(0f) }
    val edge = remember { mutableFloatStateOf(1f) }
    val latestBack = rememberUpdatedState(onBack)
    val latestChild = rememberUpdatedState(child?.instance)
    val active = child != null && backEnabled
    DisposableEffect(backHandler, active) {
        val callback = BackCallback(
            isEnabled = active,
            onBackStarted = { event: BackEvent ->
                edge.floatValue = if (event.swipeEdge == BackEvent.SwipeEdge.RIGHT) -1f else 1f
                progress.floatValue = event.progress
            },
            onBackProgressed = { event: BackEvent -> progress.floatValue = event.progress },
            onBackCancelled = { progress.floatValue = 0f },
        ) {
            progress.floatValue = 0f
            latestChild.value?.let { latestBack.value(it) }
        }
        backHandler.register(callback)
        onDispose { backHandler.unregister(callback) }
    }
    val reduced = reducedMotion
    AnimatedContent(
        targetState = child,
        modifier = modifier,
        contentKey = { it?.configuration },
        transitionSpec = { slotTransition(slide && !reduced) },
        label = "slot",
    ) { shown ->
        if (shown == null) {
            empty()
        } else {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    // The Material predictive back pose: a little smaller, nudged towards the gesture's edge.
                    val p = progress.floatValue
                    val scale = 1f - p * 0.1f
                    scaleX = scale
                    scaleY = scale
                    translationX = edge.floatValue * p * size.width * 0.06f
                    transformOrigin = TransformOrigin.Center
                    shape = androidx.compose.foundation.shape.RoundedCornerShape((p * 32).coerceAtLeast(0f))
                    clip = p > 0f
                },
            ) { Page { content(shown.instance) } }
        }
    }
}

private fun <S> AnimatedContentTransitionScope<S>.slotTransition(slide: Boolean): ContentTransform {
    val opening = initialState == null
    val closing = targetState == null
    return when {
        !slide -> fadeIn(TalkMotion.fade()) togetherWith fadeOut(TalkMotion.fade())
        opening -> slideInHorizontally(TalkMotion.moveOffset) { it } togetherWith slideOutHorizontally(TalkMotion.moveOffset) { -it / 4 } + fadeOut()
        closing -> slideInHorizontally(TalkMotion.moveOffset) { -it / 4 } + fadeIn() togetherWith slideOutHorizontally(TalkMotion.moveOffset) { it }
        else -> fadeIn(TalkMotion.fade()) togetherWith fadeOut(TalkMotion.fade())
    }.also { it.targetContentZIndex = if (closing) -1f else 1f }
}
