package com.yet.talk.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.resources.*
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.message
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import dev.icerock.moko.resources.ImageResource

/** The design's empty state (`x-empty`): an optional 48 dp icon, title, body, one action. */
@Composable
fun EmptyState(title: String, body: String? = null, icon: ImageResource? = null, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.xxl, vertical = MaterialTheme.spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        if (icon != null) TalkIcon(icon, contentDescription = null, size = MaterialTheme.sizes.iconEmpty, tint = MaterialTheme.talkColors.textSecondary)
        Text(title, style = MaterialTheme.talkTypography.headingLarge, color = MaterialTheme.talkColors.textPrimary, textAlign = TextAlign.Center)
        if (body != null) Text(body, style = MaterialTheme.talkTypography.body, color = MaterialTheme.talkColors.textSecondary, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

/** A spinner row: something is on its way. */
@Composable
fun Spinner(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(MaterialTheme.spacing.xl), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(28.dp), color = MaterialTheme.talkColors.accent, strokeWidth = 3.dp)
    }
}

/** Skeleton cards (`x-skel`): a list's first load. They breathe, unless animations are off. */
@Composable
fun SkeletonCards(count: Int = 4) {
    val pulse = rememberInfiniteTransition(label = "skeleton")
    val alpha = pulse.animateFloat(0.55f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha")
    val color = MaterialTheme.talkColors.elevated
    val spacing = MaterialTheme.spacing
    Column(Modifier.graphicsLayer { this.alpha = alpha.value }) {
        repeat(count) {
            Row(Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                Box(Modifier.size(MaterialTheme.sizes.avatarMd).clip(CircleShape).background(color))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    listOf(0.4f, 0.8f, 1f, 0.92f).forEach { width ->
                        Box(Modifier.fillMaxWidth(width).height(12.dp).clip(RoundedCornerShape(6.dp)).background(color))
                    }
                }
            }
        }
    }
}

/**
 * An error the user can dismiss (`t-alert-row`, the message in red), with Retry where it helps. It slides open and
 * closed with the error, so a list does not jump.
 */
@Composable
fun ErrorRow(error: DomainError?, onDismiss: (() -> Unit)?, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    // Keep the last error on screen while it animates away.
    val shown = remember { LastValue<DomainError>() }
    if (error != null) shown.value = error
    AnimatedVisibility(visible = error != null, modifier = modifier, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        val current = shown.value ?: return@AnimatedVisibility
        Row(
            Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Text(current.message(), style = MaterialTheme.talkTypography.body, color = MaterialTheme.talkColors.error, modifier = Modifier.weight(1f))
            if (onRetry != null) TalkButton(stringResource(MR.strings.action_retry), onRetry, ButtonStyle.Secondary)
            if (onDismiss != null) TalkIconButton(TalkIcons.Close, stringResource(MR.strings.action_dismiss), onDismiss, size = 18.dp)
        }
    }
}

/** Holds the last non-null value without being snapshot state (read only while composing the same pass). */
internal class LastValue<T : Any> { var value: T? = null }

/** A status line above a list (`t-notice`), e.g. offline or "public and not kept". */
@Composable
fun Notice(icon: ImageResource, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm)
            .clip(MaterialTheme.shapes.medium).background(MaterialTheme.talkColors.surface)
            .padding(MaterialTheme.spacing.md)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        TalkIcon(icon, contentDescription = null, size = 18.dp, tint = MaterialTheme.talkColors.textSecondary)
        Text(text, style = MaterialTheme.talkTypography.body, color = MaterialTheme.talkColors.textSecondary)
    }
}

/** A caption line under a section or list (`t-caption`). */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    Text(
        text,
        style = MaterialTheme.talkTypography.small,
        color = MaterialTheme.talkColors.textSecondary,
        textAlign = textAlign,
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
    )
}
