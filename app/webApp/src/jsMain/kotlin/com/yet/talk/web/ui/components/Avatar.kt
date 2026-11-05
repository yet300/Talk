package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.user.User
import com.yet.talk.resources.displayLabel
import com.yet.talk.resources.initials
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** Avatar sizes from the design: 32 (phone header), 40 (timeline), 48 (large), 134 (profile header). */
enum class AvatarSize(internal val className: String?) { Small("t-avatar-sm"), Medium(null), Large("x-avatar-lg"), Profile("x-avatar-profile") }

/** The user's picture, or their initials on the elevated ground while there is none. A link when [onClick] is set. */
@Composable
fun Avatar(user: User?, size: AvatarSize = AvatarSize.Medium, onClick: (() -> Unit)? = null) {
    val label = user?.displayLabel.orEmpty()
    val picture = user?.picture?.takeIf(String::isNotBlank)
    val content: @Composable () -> Unit = {
        if (picture != null) {
            Img(src = picture, alt = "") { classes("t-avatar-img") }
        } else {
            Text(user?.initials.orEmpty())
        }
    }
    if (onClick != null) {
        A(href = "#", attrs = {
            classes("x-avatar")
            size.className?.let { classes(it) }
            attr("aria-label", label)
            onClick { it.preventDefault(); it.stopPropagation(); onClick() }
        }) { content() }
    } else {
        Span({
            classes("x-avatar")
            size.className?.let { classes(it) }
            attr("aria-hidden", "true")
        }) { content() }
    }
}
