package com.yet.talk.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yet.talk.domain.model.user.User
import com.yet.talk.resources.displayLabel
import com.yet.talk.resources.handle
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkMotion
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import dev.icerock.moko.resources.ImageResource

/** A section heading (`t-section`), announced as a heading. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.talkTypography.headingMedium,
        color = MaterialTheme.talkColors.textPrimary,
        modifier = modifier.fillMaxWidth().padding(start = MaterialTheme.spacing.lg, end = MaterialTheme.spacing.lg, top = MaterialTheme.spacing.xl, bottom = MaterialTheme.spacing.sm)
            .semantics { heading() },
    )
}

/**
 * A list row (`t-row`): leading content, a title with an optional caption, trailing content. [selected] tints it
 * (`t-selected`, list-detail); [danger] makes the title red.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    selected: Boolean = false,
    danger: Boolean = false,
    captionEmphasis: Boolean = false,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    titleTrailing: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = MaterialTheme.talkColors
    val ground by animateColorAsState(if (selected) colors.surface else Color.Transparent, TalkMotion.settle(), label = "row")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.sizes.touchMin + 12.dp)
            .drawBehind { drawRect(ground) }
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.talkTypography.bodyBold,
                    color = if (danger) colors.error else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                titleTrailing?.invoke(this)
            }
            if (caption != null) {
                Text(
                    caption,
                    style = MaterialTheme.talkTypography.body,
                    color = if (captionEmphasis) colors.textPrimary else colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke(this)
    }
}

/** A person (`Search · People` row): avatar, name, handle (or [caption]); opens the profile. */
@Composable
fun PersonRow(user: User, onClick: () -> Unit, modifier: Modifier = Modifier, title: String = user.displayLabel, caption: String = user.handle, selected: Boolean = false, trailing: (@Composable RowScope.() -> Unit)? = null) {
    ListRow(title = title, caption = caption, modifier = modifier, selected = selected, onClick = onClick, leading = { Avatar(user) }, trailing = trailing)
}

/** A round icon on the elevated ground, the leading part of rows that are not people (Location channels, Teleport). */
@Composable
fun RowIcon(icon: ImageResource) {
    Box(Modifier.size(MaterialTheme.sizes.avatarMd).clip(CircleShape).drawBehindElevated(), contentAlignment = Alignment.Center) {
        TalkIcon(icon, contentDescription = null, size = 18.dp, tint = MaterialTheme.talkColors.textPrimary)
    }
}

@Composable
private fun Modifier.drawBehindElevated(): Modifier {
    val color = MaterialTheme.talkColors.elevated
    return drawBehind { drawRect(color) }
}

/** A setting with a switch (`t-switch`): the whole row toggles, and TalkBack reads it as a switch. */
@Composable
fun SwitchRow(title: String, caption: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit, icon: ImageResource? = null, enabled: Boolean = true) {
    val colors = MaterialTheme.talkColors
    Row(
        Modifier.fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        if (icon != null) TalkIcon(icon, contentDescription = null, tint = colors.textPrimary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.talkTypography.bodyBold, color = if (enabled) colors.textPrimary else colors.textSecondary)
            if (caption != null) Text(caption, style = MaterialTheme.talkTypography.body, color = colors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent),
        )
    }
}

/** A plain text action row (Erase chats, Leave): an icon and a label, red when it destroys something. */
@Composable
fun ActionRow(label: String, onClick: () -> Unit, icon: ImageResource? = null, danger: Boolean = false, note: String? = null) {
    val colors = MaterialTheme.talkColors
    val tint = if (danger) colors.error else colors.textPrimary
    Row(
        Modifier.fillMaxWidth().heightIn(min = MaterialTheme.sizes.touchMin + 8.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        if (icon != null) TalkIcon(icon, contentDescription = null, tint = tint)
        Text(label, style = MaterialTheme.talkTypography.bodyBold, color = tint, modifier = Modifier.weight(1f))
        if (note != null) Text(note, style = MaterialTheme.talkTypography.small, color = colors.accent)
    }
}
