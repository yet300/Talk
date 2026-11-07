package com.yet.talk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.rule.ProfileImageHosts
import com.yet.talk.resources.displayLabel
import com.yet.talk.resources.initials
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * The user's picture over their initials on the elevated ground. Outside the viewer's network (decision 18) the
 * picture is blurred and a GIF does not move, unless [shown].
 */
@Composable
fun Avatar(user: User?, size: Dp = MaterialTheme.sizes.avatarMd, modifier: Modifier = Modifier, shown: Boolean = false, onClick: (() -> Unit)? = null) {
    val label = user?.displayLabel.orEmpty()
    val outside = user != null && !shown && !LocalModeration.current.isInNetwork(user.id)
    val picture = user?.picture?.takeIf(String::isNotBlank)?.let { if (outside) ProfileImageHosts.still(it) else it }
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
        if (picture != null) {
            AsyncImage(
                model = picture,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().then(if (outside) Modifier.blur(12.dp) else Modifier),
            )
        }
    }
}
