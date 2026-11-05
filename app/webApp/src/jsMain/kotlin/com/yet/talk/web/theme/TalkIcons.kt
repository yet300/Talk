package com.yet.talk.web.theme

import androidx.compose.runtime.Composable
import com.yet.talk.resources.*
import com.yet.talk.resources.TalkTokens
import dev.icerock.moko.resources.ImageResource
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Span
import org.w3c.dom.HTMLSpanElement

/** The design's 24×24 stroke icon set (moko `images/ic_*.svg`). Filled variants mark an active state. */
object TalkIcons {
    val Home = MR.images.ic_home
    val Search = MR.images.ic_search
    val Activity = MR.images.ic_activity
    val Bookmark = MR.images.ic_bookmark
    val BookmarkFilled = MR.images.ic_bookmark_filled
    val User = MR.images.ic_user
    val Compose = MR.images.ic_compose
    val Reply = MR.images.ic_reply
    val Repost = MR.images.ic_repost
    val Like = MR.images.ic_like
    val LikeFilled = MR.images.ic_like_filled
    val Share = MR.images.ic_share
    val History = MR.images.ic_history
    val Back = MR.images.ic_back
    val More = MR.images.ic_more
    val Settings = MR.images.ic_settings
    val Close = MR.images.ic_close
    val Relay = MR.images.ic_relay
    val Key = MR.images.ic_key
    val Copy = MR.images.ic_copy
    val Check = MR.images.ic_check
    val Plus = MR.images.ic_plus
    val Offline = MR.images.ic_offline
    val Image = MR.images.ic_image
    val Trash = MR.images.ic_trash
}

/**
 * One icon, drawn as a CSS mask so it takes [tint] (the text colour by default), like the design's
 * `stroke: currentColor` SVGs. Pass [label] unless it is decorative.
 */
@Composable
fun TalkIcon(
    icon: ImageResource,
    label: String? = null,
    size: Float = TalkTokens.Size.iconNav,
    tint: CSSColorValue? = null,
    attrs: (AttrsScope<HTMLSpanElement>.() -> Unit)? = null,
) {
    Span({
        if (label != null) {
            attr("role", "img")
            attr("aria-label", label)
        } else {
            attr("aria-hidden", "true")
        }
        style {
            display(DisplayStyle.InlineBlock)
            width(size.px)
            height(size.px)
            property("flex-shrink", 0)
            if (tint != null) backgroundColor(tint) else property("background-color", "currentColor")
            val mask = "url(\"${icon.fileUrl}\") center / contain no-repeat"
            property("mask", mask)
            property("-webkit-mask", mask)
        }
        attrs?.invoke(this)
    })
}
