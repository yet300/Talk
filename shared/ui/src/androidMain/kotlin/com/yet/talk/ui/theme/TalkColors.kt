package com.yet.talk.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.yet.talk.resources.*
import dev.icerock.moko.resources.ColorResource

/**
 * Every colour token of the X (Twitter) design system, for one theme. Values come from
 * `core:resources` (`colors.xml`, light and dark); Material's [androidx.compose.material3.ColorScheme]
 * gets the subset its components need, the rest is read through `MaterialTheme.talkColors`.
 */
@Immutable
data class TalkColors(
    val isDark: Boolean,
    /** Page, modal and focused-input background. */
    val bg: Color,
    /** Cards, widgets, quoted posts. */
    val surface: Color,
    /** Hover states and dropdowns. */
    val elevated: Color,
    /** Dividers and card borders; decorative only. */
    val border: Color,
    val textPrimary: Color,
    /** Handles, timestamps, placeholders. */
    val textSecondary: Color,
    /** Secondary button outline. */
    val controlBorder: Color,
    val inputBorder: Color,
    val searchBg: Color,
    val hoverOverlay: Color,
    val postHover: Color,
    /** Sticky header ground, under a 12 dp blur. */
    val headerBlur: Color,
    /** Bottom navigation capsule. */
    val glassBg: Color,
    val glassSolid: Color,
    val glassPill: Color,
    val glassIcon: Color,
    /** X blue: links, focus, selected tab, primary button. */
    val accent: Color,
    val accentHover: Color,
    val onAccent: Color,
    val repost: Color,
    val like: Color,
    val error: Color,
    val warning: Color,
    val tooltipBg: Color,
    val navBadge: Color,
    val tintBlue: Color,
    val tintGreen: Color,
    val tintPink: Color,
    val modalScrim: Color,
    val mediaScrim: Color,
)

/** Reads the light or dark values regardless of the system setting, so the app can pick its own theme. */
internal fun talkColors(context: Context, dark: Boolean): TalkColors {
    val configuration = Configuration(context.resources.configuration).apply {
        val night = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
    }
    val themed = context.createConfigurationContext(configuration)
    fun color(resource: ColorResource) = Color(resource.getColor(themed))
    return with(MR.colors) {
        TalkColors(
            isDark = dark,
            bg = color(bg),
            surface = color(surface),
            elevated = color(elevated),
            border = color(border),
            textPrimary = color(text_primary),
            textSecondary = color(text_secondary),
            controlBorder = color(control_border),
            inputBorder = color(input_border),
            searchBg = color(search_bg),
            hoverOverlay = color(hover_overlay),
            postHover = color(post_hover),
            headerBlur = color(header_blur),
            glassBg = color(glass_bg),
            glassSolid = color(glass_solid),
            glassPill = color(glass_pill),
            glassIcon = color(glass_icon),
            accent = color(x_blue),
            accentHover = color(x_blue_hover),
            onAccent = color(on_accent),
            repost = color(repost_green),
            like = color(like_pink),
            error = color(error_red),
            warning = color(warning_yellow),
            tooltipBg = color(tooltip_bg),
            navBadge = color(nav_badge),
            tintBlue = color(tint_blue),
            tintGreen = color(tint_green),
            tintPink = color(tint_pink),
            modalScrim = color(modal_scrim),
            mediaScrim = color(media_scrim),
        )
    }
}
