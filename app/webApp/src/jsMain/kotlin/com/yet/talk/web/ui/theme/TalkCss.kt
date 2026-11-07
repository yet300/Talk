package com.yet.talk.web.ui.theme

import com.yet.talk.resources.*
import com.yet.talk.resources.TalkTokens
import dev.icerock.moko.graphics.Color
import dev.icerock.moko.resources.ColorResource
import kotlinx.browser.document

/**
 * The design tokens as CSS custom properties on `:root`, named as in the design file (`--x-blue`, `--space-lg`,
 * `--avatar-md`…) so `styles/x-components.css` and `styles/talk.css` read them unchanged. Colours come from
 * `core:resources`: light, dark under `prefers-color-scheme`, or forced by `data-theme` on `<html>`.
 */
internal object TalkCss {

    private val colors: List<Pair<String, ColorResource>> = with(MR.colors) {
        listOf(
            "bg" to bg, "surface" to surface, "elevated" to elevated, "border" to border,
            "text-primary" to text_primary, "text-secondary" to text_secondary,
            "control-border" to control_border, "input-border" to input_border, "search-bg" to search_bg,
            "hover-overlay" to hover_overlay, "post-hover" to post_hover, "header-blur" to header_blur,
            "glass-bg" to glass_bg, "glass-solid" to glass_solid, "glass-pill" to glass_pill, "glass-icon" to glass_icon,
            "x-blue" to x_blue, "x-blue-hover" to x_blue_hover, "on-accent" to on_accent,
            "repost-green" to repost_green, "like-pink" to like_pink, "error-red" to error_red,
            "warning-yellow" to warning_yellow, "tooltip-bg" to tooltip_bg, "nav-badge" to nav_badge,
            "tint-blue" to tint_blue, "tint-green" to tint_green, "tint-pink" to tint_pink,
            "modal-scrim" to modal_scrim, "media-scrim" to media_scrim,
            "glass-bar" to glass_bar, "glass-btn" to glass_btn, "bubble-out" to bubble_out, "bubble-theirs" to bubble_theirs,
        )
    }

    /** Shadows that differ by theme (the design's values; not colour tokens). */
    private fun shadows(dark: Boolean) = if (dark) {
        "--shadow-menu:0 0 0 1px rgba(255,255,255,.15),0 0 15px rgba(255,255,255,.1);" +
            "--shadow-modal:0 0 0 1px rgba(255,255,255,.15);" +
            "--shadow-glass:inset 0 0 0 1px rgba(255,255,255,.08),inset 0 1px 0 rgba(255,255,255,.18),0 10px 30px rgba(0,0,0,.6);" +
            "--shadow-glass-sm:inset 0 0 0 1px rgba(255,255,255,.1),inset 0 1px 0 rgba(255,255,255,.16),0 2px 8px rgba(0,0,0,.5);" +
            "--glass-edge:linear-gradient(to bottom,rgba(0,0,0,.8),rgba(0,0,0,0));"
    } else {
        "--shadow-menu:0 0 0 1px rgba(15,20,25,.15),0 0 15px rgba(15,20,25,.1);" +
            "--shadow-modal:0 0 0 1px rgba(15,20,25,.15);" +
            "--shadow-glass:inset 0 0 0 1px rgba(0,0,0,.06),inset 0 1px 0 rgba(255,255,255,.7),0 8px 24px rgba(0,0,0,.12);" +
            "--shadow-glass-sm:inset 0 0 0 1px rgba(0,0,0,.06),inset 0 1px 0 rgba(255,255,255,.8),0 2px 8px rgba(0,0,0,.12);" +
            "--glass-edge:linear-gradient(to bottom,rgba(255,255,255,.6),rgba(255,255,255,0));"
    }

    private fun Color.css() = "rgba($red, $green, $blue, ${alpha / 255.0})"

    private fun palette(dark: Boolean) =
        colors.joinToString("") { (name, color) -> "--$name:${(if (dark) color.darkColor else color.lightColor).css()};" } +
            shadows(dark) + "color-scheme:${if (dark) "dark" else "light"};"

    private fun px(value: Float) = "${value}px"

    private val tokens = buildString {
        with(TalkTokens.Spacing) {
            append("--space-xs:${px(xs)};--space-sm:${px(sm)};--space-md:${px(md)};--space-lg:${px(lg)};")
            append("--space-xl:${px(xl)};--space-2xl:${px(xxl)};--space-3xl:${px(xxxl)};--space-4xl:${px(xxxxl)};")
        }
        with(TalkTokens.Radius) {
            append("--radius-control:${px(control)};--radius-menu:${px(menu)};--radius-card:${px(card)};--radius-pill:9999px;")
        }
        with(TalkTokens.Size) {
            append("--avatar-md:${px(avatarMd)};--avatar-lg:${px(avatarLg)};--avatar-profile:${px(avatarProfile)};")
            append("--action-target:${px(actionTarget)};--icon-action:${px(iconAction)};--icon-nav:${px(iconNav)};")
            append("--button-height:${px(buttonHeight)};--button-post-height:${px(buttonPostHeight)};--fab:${px(fab)};")
            append("--bar-height:${px(barHeight)};--touch-min:${px(touchMin)};--banner-height:${px(bannerHeight)};")
            append("--media-max:${px(mediaMax)};--capsule-height:${px(capsuleHeight)};")
            append("--capsule-item-width:${px(capsuleItemWidth)};--capsule-item-height:${px(capsuleItemHeight)};")
            append("--capsule-offset:${px(capsuleOffset)};")
        }
        with(TalkTokens.Layout) {
            append("--col-left:${px(colLeft)};--col-main:${px(colMain)};--col-right:${px(colRight)};")
        }
        append("--opacity-disabled:${TalkTokens.disabledAlpha};")
        append("--shadow-fab:0 2px 10px rgba(0,0,0,.3);--focus-ring:0 0 0 2px var(--x-blue);--focus-input:0 0 0 1px var(--x-blue);")
        append("--glass-filter:blur(20px) saturate(180%);--z-nav:100;")
        append("--font-sans:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,Helvetica,Arial,sans-serif;")
        append("--mono:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;")
    }

    val text: String = buildString {
        append(":root{$tokens${palette(dark = false)}}")
        append("@media (prefers-color-scheme: dark){:root:not([data-theme=light]){${palette(dark = true)}}}")
        append(":root[data-theme=dark]{${palette(dark = true)}}")
        append("*,*::before,*::after{box-sizing:border-box}")
        append(
            "body{margin:0;background:var(--bg);color:var(--text-primary);font-family:var(--font-sans);" +
                "font-size:15px;line-height:20px}",
        )
        append("a{color:inherit;text-decoration:none}")
    }

    /** Adds the token stylesheet to `<head>` once, before the component stylesheets linked in index.html. */
    fun install() {
        if (document.getElementById(ID) != null) return
        val style = document.createElement("style")
        style.id = ID
        style.textContent = text
        document.head?.insertBefore(style, document.head?.firstChild)
    }

    private const val ID = "talk-tokens"
}
