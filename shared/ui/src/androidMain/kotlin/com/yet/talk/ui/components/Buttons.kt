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
