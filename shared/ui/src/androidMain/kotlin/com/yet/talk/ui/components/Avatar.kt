package com.yet.talk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.yet.talk.domain.model.user.User
import com.yet.talk.resources.displayLabel
import com.yet.talk.resources.initials
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/** The user's initials on the elevated ground; pictures come with image loading in the feed step. */
@Composable
fun Avatar(user: User?, size: Dp = MaterialTheme.sizes.avatarMd, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val label = user?.displayLabel.orEmpty()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.talkColors.elevated)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(user?.initials.orEmpty(), style = MaterialTheme.talkTypography.small, color = MaterialTheme.talkColors.textPrimary)
    }
}
