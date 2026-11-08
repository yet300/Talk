package com.yet.talk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.yet.talk.resources.*
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkShapes
import com.yet.talk.ui.theme.talkTypography

/** The design's buttons: Post (blue, large), Primary (inverted), Secondary (outlined). */
enum class ButtonStyle { Post, Primary, Secondary }

@Composable
fun TalkButton(
    text: String,
    onClick: () -> Unit,
    style: ButtonStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
) {
    val colors = MaterialTheme.talkColors
    val shape = MaterialTheme.talkShapes.pill
    val sized = modifier
        .heightIn(min = if (style == ButtonStyle.Post) MaterialTheme.sizes.buttonPostHeight else MaterialTheme.sizes.touchMin)
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
    val label: @Composable () -> Unit = {
        Text(text, style = if (style == ButtonStyle.Post) MaterialTheme.talkTypography.postButtonLabel else MaterialTheme.talkTypography.bodyBold)
    }
    when (style) {
        ButtonStyle.Post -> Button(onClick, sized, enabled, shape, ButtonDefaults.buttonColors(colors.accent, colors.onAccent)) { label() }
        ButtonStyle.Primary -> Button(onClick, sized, enabled, shape, ButtonDefaults.buttonColors(colors.textPrimary, colors.bg)) { label() }
        ButtonStyle.Secondary -> OutlinedButton(
            onClick, sized, enabled, shape,
            ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
            border = BorderStroke(MaterialTheme.sizes.hairline, colors.controlBorder),
        ) { label() }
    }
}

/** A plain round icon button (`t-icon-btn`) off the glass: 44 dp target, [label] is its accessible name. */
@Composable
fun TalkIconButton(
    icon: dev.icerock.moko.resources.ImageResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = MaterialTheme.sizes.iconNav,
    enabled: Boolean = true,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.talkColors.textPrimary,
) {
    androidx.compose.material3.IconButton(onClick = onClick, enabled = enabled, modifier = modifier.androidxSemanticsLabel(label)) {
        com.yet.talk.ui.theme.TalkIcon(icon, contentDescription = null, size = size, tint = if (enabled) tint else MaterialTheme.talkColors.textSecondary)
    }
}

private fun Modifier.androidxSemanticsLabel(label: String): Modifier =
    semantics { contentDescription = label }

/** A text-only action (`t-text-btn`), accent coloured. */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false, enabled: Boolean = true) {
    androidx.compose.material3.TextButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(text, style = MaterialTheme.talkTypography.bodyBold, color = if (danger) MaterialTheme.talkColors.error else MaterialTheme.talkColors.accent)
    }
}

/** Follow / Following: the button crossfades between the two (inverted, then outlined) as the state flips. */
@Composable
fun FollowButton(following: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.animation.Crossfade(following, modifier, animationSpec = com.yet.talk.ui.theme.TalkMotion.fade(), label = "follow") { on ->
        TalkButton(
            com.yet.talk.ui.theme.stringResource(if (on) MR.strings.action_following else MR.strings.action_follow),
            onClick,
            if (on) ButtonStyle.Secondary else ButtonStyle.Primary,
        )
    }
}
