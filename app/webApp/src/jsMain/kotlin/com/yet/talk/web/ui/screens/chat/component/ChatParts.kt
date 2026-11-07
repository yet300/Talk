package com.yet.talk.web.ui.screens.chat.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.user.User
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.components.AvatarSize
import org.jetbrains.compose.web.dom.Span

/** A chat's title: a group's name, else its members' names. */
fun chatTitle(name: String?, members: List<User>): String =
    name?.takeIf(String::isNotBlank) ?: members.joinToString(", ") { it.displayLabel }

/** One avatar for a person, two stacked (`t-av-stack`) for a group. */
@Composable
fun ChatAvatar(members: List<User>, small: Boolean = false) {
    if (members.size > 1) {
        Span({
            classes("t-av-stack")
            if (small) classes("t-av-stack-sm")
            attr("aria-hidden", "true")
        }) {
            Avatar(members[0])
            Avatar(members[1])
        }
    } else {
        Avatar(members.firstOrNull(), if (small) AvatarSize.Small else AvatarSize.Medium)
    }
}
