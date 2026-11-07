package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.rule.ProfileImageHosts
import com.yet.talk.resources.displayLabel
import com.yet.talk.resources.initials
import com.yet.talk.web.ui.components.moderation.LocalModeration
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** Avatar sizes from the design: 32 (phone header), 40 (timeline), 48 (large), 134 (profile header). */
enum class AvatarSize(internal val className: String?) { Small("t-avatar-sm"), Medium(null), Large("x-avatar-lg"), Profile("x-avatar-profile") }

/**
 * The user's picture, or their initials on the elevated ground while there is none. A link when [onClick] is set.
 * Outside the viewer's network (decision 18) the picture is blurred and a GIF does not move, unless [shown].
 */
@Composable
fun Avatar(user: User?, size: AvatarSize = AvatarSize.Medium, onClick: (() -> Unit)? = null, shown: Boolean = false) {
    val label = user?.displayLabel.orEmpty()
    val outside = user != null && !shown && !LocalModeration.current.isInNetwork(user.id)
    val picture = user?.picture?.takeIf(String::isNotBlank)?.let { if (outside) ProfileImageHosts.still(it) else it }
    val content: @Composable () -> Unit = {
        if (picture != null) {
            Img(src = picture, alt = "") { classes("t-avatar-img"); if (outside) classes("t-blur") }
        } else {
            Text(user?.initials.orEmpty())
        }
    }
    if (onClick != null) {
        A(href = "#", attrs = {
            classes("x-avatar", "t-av-clip")
            size.className?.let { classes(it) }
            attr("aria-label", label)
            onClick { it.preventDefault(); it.stopPropagation(); onClick() }
        }) { content() }
    } else {
        Span({
            classes("x-avatar", "t-av-clip")
            size.className?.let { classes(it) }
            attr("aria-hidden", "true")
        }) { content() }
    }
}
