package com.yet.talk.ui.screens.chat.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yet.talk.domain.model.user.User
import com.yet.talk.resources.displayLabel
import com.yet.talk.ui.components.Avatar
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.talkColors

/** A chat's title: a group's name, else its members' names. */
fun chatTitle(name: String?, members: List<User>): String =
    name?.takeIf(String::isNotBlank) ?: members.joinToString(", ") { it.displayLabel }

/** One avatar for a person, two stacked (`t-av-stack`) for a group. */
@Composable
fun ChatAvatar(members: List<User>, size: Dp = MaterialTheme.sizes.avatarMd) {
    if (members.size > 1) {
        val small = size * 0.72f
        Box(Modifier.size(size)) {
            Avatar(members[0], size = small)
            Avatar(members[1], size = small, modifier = Modifier.offset(x = size - small, y = size - small).border(2.dp, MaterialTheme.talkColors.bg, CircleShape))
        }
    } else {
        Avatar(members.firstOrNull(), size = size)
    }
}
