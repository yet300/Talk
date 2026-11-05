package com.yet.talk.web.theme

import com.yet.talk.resources.*
import com.yet.talk.resources.TalkTokens
import dev.icerock.moko.graphics.Color
import dev.icerock.moko.resources.ColorResource
import kotlinx.browser.document

/**
 * The design tokens as CSS custom properties on `:root`: colours from `core:resources` (light, dark under
 * `prefers-color-scheme`, or forced with `data-theme` on `<html>`), plus spacing, radii and the type scale.
 */
internal object TalkCss {

    private val colors: List<Pair<String, ColorResource>> = with(MR.colors) {
        listOf(
            "bg" to bg, "surface" to surface, "elevated" to elevated, "border" to border,
            "text-primary" to text_primary, "text-secondary" to text_secondary,
            "control-border" to control_border, "input-border" to input_border, "search-bg" to search_bg,
            "hover-overlay" to hover_overlay, "post-hover" to post_hover, "header-blur" to header_blur,
            "glass-bg" to glass_bg, "glass-solid" to glass_solid, "glass-pill" to glass_pill, "glass-icon" to glass_icon,
            "accent" to x_blue, "accent-hover" to x_blue_hover, "on-accent" to on_accent,
            "repost" to repost_green, "like" to like_pink, "error" to error_red, "warning" to warning_yellow,
            "tooltip-bg" to tooltip_bg, "nav-badge" to nav_badge,
            "tint-blue" to tint_blue, "tint-green" to tint_green, "tint-pink" to tint_pink,
            "modal-scrim" to modal_scrim, "media-scrim" to media_scrim,
        )
    }

    private fun Color.css() = "rgba($red, $green, $blue, ${alpha / 255.0})"

    private fun palette(dark: Boolean) =
        colors.joinToString("") { (name, color) -> "--$name:${(if (dark) color.darkColor else color.lightColor).css()};" }

    private fun type(name: String, style: TalkTokens.TextStyle) =
        "--type-$name-size:${style.size}px;--type-$name-line:${style.lineHeight}px;" +
            "--type-$name-weight:${style.weight};--type-$name-tracking:${style.letterSpacing}px;"

    private val tokens = buildString {
        with(TalkTokens.Spacing) {
            append("--space-xs:${xs}px;--space-sm:${sm}px;--space-md:${md}px;--space-lg:${lg}px;")
            append("--space-xl:${xl}px;--space-2xl:${xxl}px;--space-3xl:${xxxl}px;--space-4xl:${xxxxl}px;")
        }
        with(TalkTokens.Radius) {
            append("--radius-control:${control}px;--radius-menu:${menu}px;--radius-card:${card}px;--radius-pill:9999px;")
        }
        with(TalkTokens.Type) {
            listOf(
                "display" to display, "heading-large" to headingLarge, "heading-medium" to headingMedium,
                "body" to body, "body-bold" to bodyBold, "caption" to caption, "small" to small, "tiny" to tiny,
                "reading" to reading, "code" to code, "code-block" to codeBlock, "nav-label" to navLabel,
                "tab-label" to tabLabel, "input" to inputText, "post-button" to postButtonLabel, "wordmark" to wordmark,
            ).forEach { (name, style) -> append(type(name, style)) }
        }
        append("--font-sans:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,Helvetica,Arial,sans-serif;")
        append("--font-mono:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;")
    }

    val text: String = buildString {
        append(":root{$tokens${palette(dark = false)}color-scheme:light}")
        append("@media (prefers-color-scheme: dark){:root:not([data-theme=light]){${palette(dark = true)}color-scheme:dark}}")
        append(":root[data-theme=dark]{${palette(dark = true)}color-scheme:dark}")
        append("*,*::before,*::after{box-sizing:border-box}")
        append(
            "body{margin:0;background:var(--bg);color:var(--text-primary);font-family:var(--font-sans);" +
                "font-size:var(--type-body-size);line-height:var(--type-body-line)}",
        )
        append("a{color:inherit;text-decoration:none}")
    }

    /** Adds the stylesheet to `<head>` once. */
    fun install() {
        if (document.getElementById(ID) != null) return
        val style = document.createElement("style")
        style.id = ID
        style.textContent = text
        document.head?.appendChild(style)
    }

    private const val ID = "talk-tokens"
}
