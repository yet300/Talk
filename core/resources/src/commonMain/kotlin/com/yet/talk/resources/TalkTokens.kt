package com.yet.talk.resources

/**
 * Non-colour design tokens of the X (Twitter) design system used by the Claude Design file "Talk".
 * Colours, strings and icons are moko resources ([MR]); these are plain numbers in dp/pt/px so every
 * platform (Compose, SwiftUI, Compose HTML) builds its theme from one source.
 */
object TalkTokens {

    object Spacing {
        /** Icon spacing, the base unit. */
        const val xs = 4f
        const val sm = 8f
        /** Post card block padding, nav item and card internal padding. */
        const val md = 12f
        /** Post card inline padding, modal padding, section gaps. */
        const val lg = 16f
        const val xl = 20f
        const val xxl = 24f
        const val xxxl = 32f
        const val xxxxl = 48f
    }

    object Radius {
        /** Inputs and tooltips. */
        const val control = 4f
        const val menu = 12f
        /** Cards, quoted posts, media, modals. */
        const val card = 16f
        /** Buttons, avatars, search, tags. */
        const val pill = 9999f
    }

    object Size {
        const val avatarMd = 40f
        const val avatarLg = 48f
        const val avatarProfile = 134f
        const val actionTarget = 34.75f
        const val iconAction = 18f
        const val iconNav = 24f
        const val iconEmpty = 48f
        const val logo = 28f
        const val buttonHeight = 36f
        const val buttonPostHeight = 52f
        const val fab = 56f
        const val barHeight = 53f
        const val touchMin = 44f
        const val bannerHeight = 200f
        const val mediaMax = 510f
        const val capsuleHeight = 62f
        const val capsuleItemWidth = 84f
        const val capsuleItemHeight = 54f
        const val capsuleOffset = 28f
        /** Divider and border width. */
        const val hairline = 1f
        /** Stroke width of the icon set. */
        const val iconStroke = 1.75f
    }

    object Layout {
        const val colLeft = 275f
        const val colLeftCollapsed = 68f
        const val colMain = 600f
        const val colRight = 350f
        const val bpMobileL = 688f
        const val bpTablet = 1005f
        const val bpDesktop = 1100f
        const val bpDesktopXl = 1400f
    }

    const val disabledAlpha = 0.5f

    /** One text style: size and line height in sp/pt/px, CSS weight, letter spacing in the same unit. */
    data class TextStyle(val size: Float, val lineHeight: Float, val weight: Int, val letterSpacing: Float = 0f)

    object Type {
        /** Trending and hero figures; also the reader's h1. */
        val display = TextStyle(31f, 36f, 800, -0.5f)
        /** Page and profile titles; the wordmark. */
        val headingLarge = TextStyle(23f, 28f, 700)
        /** Section and widget titles, card titles, bar titles. */
        val headingMedium = TextStyle(20f, 24f, 700)
        /** Post content, handles, timestamps. */
        val body = TextStyle(15f, 20f, 400)
        /** Display names, button labels, emphasis. */
        val bodyBold = TextStyle(15f, 20f, 700)
        val caption = TextStyle(13f, 16f, 400)
        val small = TextStyle(12f, 16f, 400, 0.2f)
        /** The floor: nothing smaller. */
        val tiny = TextStyle(11f, 14f, 400, 0.2f)

        /** Long-form reading (17/28), a gap-fill in the design: the system's scale stops at 15/20. */
        val reading = TextStyle(17f, 28f, 400)
        val code = TextStyle(15f, 24f, 400)
        val codeBlock = TextStyle(13f, 20f, 400)

        val navLabel = TextStyle(20f, 24f, 400)
        val tabLabel = TextStyle(15f, 20f, 500)
        val inputText = TextStyle(17f, 24f, 400)
        val postButtonLabel = TextStyle(17f, 20f, 700)
        val wordmark = TextStyle(23f, 28f, 800, -0.5f)
    }
}
