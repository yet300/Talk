package com.yet.talk.web.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.resources.TalkTokens
import com.yet.talk.resources.toStringDesc
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.StringDesc
import dev.icerock.moko.resources.provider.JsStringProvider
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.fontFamily
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.letterSpacing
import org.jetbrains.compose.web.css.lineHeight
import org.jetbrains.compose.web.css.px

private val LocalStrings = staticCompositionLocalOf<JsStringProvider> { error("Strings are provided by TalkTheme") }

/** Installs the token stylesheet and provides the loaded strings. */
@Composable
fun TalkTheme(strings: JsStringProvider, content: @Composable () -> Unit) {
    TalkCss.install()
    CompositionLocalProvider(LocalStrings provides strings, content = content)
}

/** A moko string (`MR.strings.*`) in the browser's locale. */
@Composable
fun stringResource(resource: StringResource, vararg args: Any): String =
    if (args.isEmpty()) resource.localized(LocalStrings.current, null) else resource.localized(LocalStrings.current, null, *args)

/** A moko plural (`MR.plurals.*`) with [count] as its argument. */
@Composable
fun pluralResource(resource: PluralsResource, count: Int): String =
    resource.localized(LocalStrings.current, null, count, count)

@Composable
fun StringDesc.text(): String = toLocalizedString(LocalStrings.current)

/** The user-facing message for an error from the domain layer. */
@Composable
fun DomainError.message(): String = toStringDesc().text()

/** Colour tokens as CSS variables; they switch with the theme without recomposition. */
object TalkColor {
    private fun v(name: String): CSSColorValue = Color("var(--$name)")
    val bg = v("bg")
    val surface = v("surface")
    val elevated = v("elevated")
    val border = v("border")
    val textPrimary = v("text-primary")
    val textSecondary = v("text-secondary")
    val controlBorder = v("control-border")
    val inputBorder = v("input-border")
    val searchBg = v("search-bg")
    val hoverOverlay = v("hover-overlay")
    val postHover = v("post-hover")
    val headerBlur = v("header-blur")
    val glassBg = v("glass-bg")
    val glassSolid = v("glass-solid")
    val glassPill = v("glass-pill")
    val glassIcon = v("glass-icon")
    val accent = v("accent")
    val accentHover = v("accent-hover")
    val onAccent = v("on-accent")
    val repost = v("repost")
    val like = v("like")
    val error = v("error")
    val warning = v("warning")
    val tooltipBg = v("tooltip-bg")
    val navBadge = v("nav-badge")
    val tintBlue = v("tint-blue")
    val tintGreen = v("tint-green")
    val tintPink = v("tint-pink")
    val modalScrim = v("modal-scrim")
    val mediaScrim = v("media-scrim")
}

/** `TalkTokens.Spacing` in px. */
object TalkSpacing {
    val xs = TalkTokens.Spacing.xs.px
    val sm = TalkTokens.Spacing.sm.px
    val md = TalkTokens.Spacing.md.px
    val lg = TalkTokens.Spacing.lg.px
    val xl = TalkTokens.Spacing.xl.px
    val xxl = TalkTokens.Spacing.xxl.px
    val xxxl = TalkTokens.Spacing.xxxl.px
    val xxxxl = TalkTokens.Spacing.xxxxl.px
}

/** `TalkTokens.Radius` in px. */
object TalkRadius {
    val control = TalkTokens.Radius.control.px
    val menu = TalkTokens.Radius.menu.px
    val card = TalkTokens.Radius.card.px
    val pill = 9999.px
}

/** Applies one of the design's text styles (`TalkTokens.Type.*`). */
fun StyleScope.textStyle(style: TalkTokens.TextStyle, monospace: Boolean = false) {
    if (monospace) fontFamily("ui-monospace", "SFMono-Regular", "Menlo", "Consolas", "monospace")
    fontSize(style.size.px)
    lineHeight(style.lineHeight.px)
    fontWeight(style.weight)
    letterSpacing(style.letterSpacing.px)
}
