package com.yet.talk.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkMotion
import com.yet.talk.ui.theme.reducedMotion
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkShapes
import com.yet.talk.ui.theme.talkTypography
import dev.icerock.moko.resources.ImageResource

/**
 * An outlined pill action with an icon and a label (`x-action` with text: the video page's Like, Watch later, Share).
 * [onColor] set means it is on: the colour eases in and the icon pops once when the finger turns it on.
 */
@Composable
fun ToggleAction(icon: ImageResource, label: String, onColor: Color?, onClick: () -> Unit) {
    val on = onColor != null
    val colors = MaterialTheme.talkColors
    val tint by animateColorAsState(onColor ?: colors.textPrimary, TalkMotion.settle(), label = "tint")
    val scale = remember { Animatable(1f) }
    val seen = remember { booleanArrayOf(on) }
    val reduced = reducedMotion
    LaunchedEffect(on) {
        if (on && !seen[0] && !reduced) {
            scale.snapTo(0.6f)
            scale.animateTo(1f, TalkMotion.pop())
        }
        seen[0] = on
    }
    val shape = MaterialTheme.talkShapes.pill
    Row(
        Modifier.heightIn(min = MaterialTheme.sizes.touchMin).clip(shape).border(BorderStroke(MaterialTheme.sizes.hairline, colors.controlBorder), shape)
            .toggleable(value = on, role = Role.Button, onValueChange = { onClick() })
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TalkIcon(icon, contentDescription = null, size = 18.dp, tint = tint, modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value })
        Text(label, style = MaterialTheme.talkTypography.bodyBold, color = tint)
    }
}
