package com.yet.talk.web.ui.theme

import androidx.compose.runtime.Composable
import com.yet.talk.resources.*
import com.yet.talk.resources.TalkTokens
import dev.icerock.moko.resources.ImageResource
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import org.jetbrains.compose.web.svg.height
import org.jetbrains.compose.web.svg.width
import org.w3c.dom.svg.SVGElement

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
    val GitHub = MR.images.ic_github
    val Star = MR.images.ic_star
    val Fork = MR.images.ic_fork
    val Globe = MR.images.ic_globe
}

/**
 * One icon, drawn inline as the design does (`.t-i`: 24×24 viewBox, 1.75 stroke in currentColor), so it takes
 * [tint] or the text colour at once; filled variants (`*_filled`) fill with the same colour. Pass [label]
 * unless it is decorative.
 */
@Composable
fun TalkIcon(
    icon: ImageResource,
    label: String? = null,
    size: Float = TalkTokens.Size.iconNav,
    tint: CSSColorValue? = null,
    attrs: (AttrsScope<SVGElement>.() -> Unit)? = null,
) {
    val path = IconPaths[icon.fileName.substringAfterLast('/').substringBefore('.')]
    Svg(viewBox = "0 0 24 24", attrs = {
        classes("t-i")
        width(size)
        height(size)
        if (label != null) {
            attr("role", "img")
            attr("aria-label", label)
        } else {
            attr("aria-hidden", "true")
        }
        if (tint != null) style { color(tint) }
        attrs?.invoke(this)
    }) {
        if (path != null) {
            Path(path.d, attrs = {
                if (path.filled) attr("fill", "currentColor")
                // Marks drawn as shapes only (the GitHub mark) must not get the set's outline.
                if (!path.stroked) attr("stroke", "none")
            })
        }
    }
}
