package com.yet.talk.ui.components.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.glass.hazeGlass

/**
 * A floating glass segmented capsule (`t-seg-capsule`, Home's Following · Popular · Latest): the selection is a pill
 * that slides with a spring, as in iOS 26.
 */
@Composable
fun <T> GlassSegments(items: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.talkColors
    val index = items.indexOf(selected).coerceAtLeast(0)
    val pill by animateDpAsState(SegmentWidth * index, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow), label = "segment")
    Box(
        modifier = modifier
            .hazeGlass(input = HazeInput.Backdrop(LocalGlassSource.current), style = pillGlass())
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .offset(x = pill)
                .width(SegmentWidth)
                .height(MaterialTheme.sizes.touchMin)
                .clip(RoundedCornerShape(percent = 50))
                .background(colors.glassPill),
        )
        Row(Modifier.selectableGroup()) {
            items.forEach { item ->
                val active = item == selected
                Box(
                    Modifier
                        .width(SegmentWidth)
                        .height(MaterialTheme.sizes.touchMin)
                        .clip(RoundedCornerShape(percent = 50))
                        .selectable(selected = active, role = Role.Tab, indication = null, interactionSource = null) { onSelect(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(item),
                        style = if (active) MaterialTheme.talkTypography.bodyBold else MaterialTheme.talkTypography.body,
                        color = if (active) colors.textPrimary else colors.glassIcon,
                    )
                }
            }
        }
    }
}

private val SegmentWidth = 96.dp
