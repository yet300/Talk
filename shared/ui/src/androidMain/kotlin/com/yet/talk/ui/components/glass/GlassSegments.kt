package com.yet.talk.ui.components.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yet.talk.ui.theme.TalkMotion
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.glass.hazeGlass
import kotlin.math.roundToInt

/**
 * A floating glass segmented capsule (`t-seg-capsule`: Home's Following · Popular · Latest, a profile's tabs, Chats ·
 * Requests). The selection is a pill that slides with a spring, as in iOS 26; with [position] (the pager's page plus
 * its offset) it follows a swipe 1:1. The position is read in the layout phase only, so a swipe does not recompose.
 */
@Composable
fun <T> GlassSegments(
    items: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    position: (() -> Float?)? = null,
    segmentWidth: androidx.compose.ui.unit.Dp = SegmentWidth,
) {
    val colors = MaterialTheme.talkColors
    val index = items.indexOf(selected).coerceAtLeast(0)
    val settled = animateFloatAsState(index.toFloat(), spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow), label = "segment")
    val follow by rememberUpdatedState(position)
    Box(
        modifier = modifier
            .hazeGlass(input = HazeInput.Backdrop(LocalGlassSource.current), style = pillGlass())
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .offset {
                    val at = (follow?.invoke() ?: settled.value).coerceIn(0f, (items.size - 1).coerceAtLeast(0).toFloat())
                    IntOffset((segmentWidth.toPx() * at).roundToInt(), 0)
                }
                .width(segmentWidth)
                .height(MaterialTheme.sizes.touchMin)
                .clip(RoundedCornerShape(percent = 50))
                .background(colors.glassPill),
        )
        Row(Modifier.selectableGroup()) {
            items.forEach { item ->
                val active = item == selected
                val text by animateColorAsState(if (active) colors.textPrimary else colors.glassIcon, TalkMotion.settle(), label = "label")
                Box(
                    Modifier
                        .width(segmentWidth)
                        .height(MaterialTheme.sizes.touchMin)
                        .clip(RoundedCornerShape(percent = 50))
                        .selectable(selected = active, role = Role.Tab, indication = null, interactionSource = null) { onSelect(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label(item), style = MaterialTheme.talkTypography.bodyBold, color = text, maxLines = 1)
                }
            }
        }
    }
}

private val SegmentWidth = 96.dp
