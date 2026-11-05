package com.yet.talk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.yet.talk.resources.TalkTokens

/** The design's type scale (`TalkTokens.Type`) in the system sans (Roboto on Android). */
@Immutable
data class TalkTypography(
    val display: TextStyle,
    val headingLarge: TextStyle,
    val headingMedium: TextStyle,
    val body: TextStyle,
    val bodyBold: TextStyle,
    val caption: TextStyle,
    val small: TextStyle,
    val tiny: TextStyle,
    /** Long-form markdown body. */
    val reading: TextStyle,
    val code: TextStyle,
    val codeBlock: TextStyle,
    val navLabel: TextStyle,
    val tabLabel: TextStyle,
    val inputText: TextStyle,
    val postButtonLabel: TextStyle,
    val wordmark: TextStyle,
)

private fun TalkTokens.TextStyle.toCompose(family: FontFamily = FontFamily.Default) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = FontWeight(weight),
    // Tokens are in px; letter spacing relative to the size keeps it right under font scaling.
    letterSpacing = (letterSpacing / size).em,
)

internal val DefaultTalkTypography = with(TalkTokens.Type) {
    TalkTypography(
        display = display.toCompose(),
        headingLarge = headingLarge.toCompose(),
        headingMedium = headingMedium.toCompose(),
        body = body.toCompose(),
        bodyBold = bodyBold.toCompose(),
        caption = caption.toCompose(),
        small = small.toCompose(),
        tiny = tiny.toCompose(),
        reading = reading.toCompose(),
        code = code.toCompose(FontFamily.Monospace),
        codeBlock = codeBlock.toCompose(FontFamily.Monospace),
        navLabel = navLabel.toCompose(),
        tabLabel = tabLabel.toCompose(),
        inputText = inputText.toCompose(),
        postButtonLabel = postButtonLabel.toCompose(),
        wordmark = wordmark.toCompose(),
    )
}

/** Material components read these slots: text fields bodyLarge, buttons labelLarge, top bars titleLarge. */
internal fun TalkTypography.toMaterial() = Typography(
    displayLarge = display,
    displayMedium = display,
    displaySmall = display,
    headlineLarge = headingLarge,
    headlineMedium = headingLarge,
    headlineSmall = headingLarge,
    titleLarge = headingMedium,
    titleMedium = bodyBold,
    titleSmall = bodyBold,
    bodyLarge = inputText,
    bodyMedium = body,
    bodySmall = caption,
    labelLarge = bodyBold,
    labelMedium = tabLabel,
    labelSmall = small,
)
