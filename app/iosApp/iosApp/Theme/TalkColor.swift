import SwiftUI
import SharedLogic

/// Colour tokens of the X (Twitter) design system from `core:resources` (`colors.xml`).
/// Each is a dynamic asset-catalog colour, so it follows light and dark mode (or `.preferredColorScheme`).
enum TalkColor {
    private static func color(_ resource: SharedLogic.ColorResource) -> Color { Color(uiColor: resource.getUIColor()) }
    private static let mr = MR.colors.shared

    /// Page, modal and focused-input background.
    static let bg = color(mr.bg)
    /// Cards, widgets, quoted posts.
    static let surface = color(mr.surface)
    /// Hover states and dropdowns.
    static let elevated = color(mr.elevated)
    /// Dividers and card borders; decorative only.
    static let border = color(mr.border)
    static let textPrimary = color(mr.text_primary)
    /// Handles, timestamps, placeholders.
    static let textSecondary = color(mr.text_secondary)
    static let controlBorder = color(mr.control_border)
    static let inputBorder = color(mr.input_border)
    static let searchBg = color(mr.search_bg)
    static let hoverOverlay = color(mr.hover_overlay)
    static let postHover = color(mr.post_hover)
    static let headerBlur = color(mr.header_blur)
    static let glassBg = color(mr.glass_bg)
    static let glassSolid = color(mr.glass_solid)
    static let glassPill = color(mr.glass_pill)
    static let glassIcon = color(mr.glass_icon)
    /// X blue: links, focus, selected tab, primary button.
    static let accent = color(mr.x_blue)
    static let accentHover = color(mr.x_blue_hover)
    static let onAccent = color(mr.on_accent)
    static let repost = color(mr.repost_green)
    static let like = color(mr.like_pink)
    static let error = color(mr.error_red)
    static let warning = color(mr.warning_yellow)
    static let tooltipBg = color(mr.tooltip_bg)
    static let navBadge = color(mr.nav_badge)
    static let tintBlue = color(mr.tint_blue)
    static let tintGreen = color(mr.tint_green)
    static let tintPink = color(mr.tint_pink)
    static let modalScrim = color(mr.modal_scrim)
    static let mediaScrim = color(mr.media_scrim)
}
